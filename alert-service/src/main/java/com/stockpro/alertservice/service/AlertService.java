package com.stockpro.alertservice.service;

import com.stockpro.alertservice.dto.request.AcknowledgeAlertRequest;
import com.stockpro.alertservice.dto.request.AlertSearchRequest;
import com.stockpro.alertservice.dto.request.CreateAlertRequest;
import com.stockpro.alertservice.dto.request.CreateBroadcastAlertRequest;
import com.stockpro.alertservice.dto.request.DismissAlertRequest;
import com.stockpro.alertservice.dto.response.AlertAnalyticsResponse;
import com.stockpro.alertservice.dto.response.AlertAnalyticsResponse.AlertEntityCount;
import com.stockpro.alertservice.dto.response.AlertResponse;
import com.stockpro.alertservice.dto.response.AlertSummaryResponse;
import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import com.stockpro.alertservice.events.AlertEvent;
import com.stockpro.alertservice.events.MovementAlertEvent;
import com.stockpro.alertservice.events.PurchaseAlertEvent;
import com.stockpro.alertservice.events.StockAlertEvent;
import com.stockpro.alertservice.events.SupplierAlertEvent;
import com.stockpro.alertservice.exception.AlertNotFoundException;
import com.stockpro.alertservice.exception.InvalidAlertException;
import com.stockpro.alertservice.mail.EmailNotificationService;
import com.stockpro.alertservice.rabbitmq.AlertEventPublisher;
import com.stockpro.alertservice.repository.AlertRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AlertRepository alertRepository;
    private final AlertValidationService validationService;
    private final AlertMapper alertMapper;
    private final AlertEventPublisher alertEventPublisher;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public AlertResponse createAlert(CreateAlertRequest request, Long actorId) {
        validationService.validateCreateAlert(request);
        Alert alert = buildAlert(request);
        Alert saved = alertRepository.save(alert);
        log.info("Alert created alertId={} type={} severity={} recipientId={} recipientRole={}",
                saved.getAlertId(), saved.getType(), saved.getSeverity(), saved.getRecipientId(), saved.getRecipientRole());
        publish("alert.created", toEvent(saved));
        maybeSendCriticalEmail(saved);
        return alertMapper.toResponse(saved);
    }

    @Transactional
    public List<AlertResponse> createBroadcastAlert(CreateBroadcastAlertRequest request, Long actorId) {
        validationService.validateBroadcast(request);
        List<AlertResponse> responses = new ArrayList<>();

        if (request.getRecipientRoles() != null) {
            for (String role : request.getRecipientRoles()) {
                CreateAlertRequest create = new CreateAlertRequest();
                create.setRecipientRole(role);
                create.setType(AlertType.SYSTEM_BROADCAST);
                create.setSeverity(request.getSeverity());
                create.setChannel(defaultChannel(request.getSeverity(), null));
                create.setTitle(request.getTitle());
                create.setMessage(request.getMessage());
                create.setExpiresAt(request.getExpiresAt());
                create.setActionUrl(request.getActionUrl());
                responses.add(createAlert(create, actorId));
            }
        }

        if (request.getRecipientIds() != null) {
            for (Long recipientId : request.getRecipientIds()) {
                CreateAlertRequest create = new CreateAlertRequest();
                create.setRecipientId(recipientId);
                create.setType(AlertType.SYSTEM_BROADCAST);
                create.setSeverity(request.getSeverity());
                create.setChannel(defaultChannel(request.getSeverity(), null));
                create.setTitle(request.getTitle());
                create.setMessage(request.getMessage());
                create.setExpiresAt(request.getExpiresAt());
                create.setActionUrl(request.getActionUrl());
                responses.add(createAlert(create, actorId));
            }
        }

        log.info("Broadcast alert created count={} actorId={}", responses.size(), actorId);
        return responses;
    }

    public AlertResponse getAlertById(Long alertId, Long userId, String role, boolean isAdmin) {
        return alertMapper.toResponse(authorizeAlert(alertId, userId, role, isAdmin));
    }

    public AlertResponse getAlertByNumber(String alertNumber, Long userId, String role, boolean isAdmin) {
        return alertRepository.findByAlertNumber(alertNumber)
                .map(alert -> authorizeAlert(alert, userId, role, isAdmin))
                .map(alertMapper::toResponse)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found"));
    }

    public Page<AlertResponse> getMyAlerts(Long userId, String role, AlertSearchRequest request) {
        Pageable pageable = pageRequest(
                valueOrDefault(request.getPage(), 0),
                valueOrDefault(request.getSize(), 10),
                request.getSortBy(),
                request.getSortDir());
        Specification<Alert> spec = myAlertsSpec(userId, role).and(searchSpec(request));
        return alertRepository.findAll(spec, pageable).map(alertMapper::toResponse);
    }

    public Page<AlertResponse> searchAlerts(AlertSearchRequest request, Long userId, String role, boolean isAdmin) {
        Pageable pageable = pageRequest(valueOrDefault(request.getPage(), 0), valueOrDefault(request.getSize(), 10),
                request.getSortBy(), request.getSortDir());
        Specification<Alert> spec = searchSpec(request);
        if (!isAdmin) {
            spec = spec.and(myAlertsSpec(userId, role));
        }
        return alertRepository.findAll(spec, pageable).map(alertMapper::toResponse);
    }

    @Transactional
    public AlertResponse markAsRead(Long alertId, Long userId, String role, boolean isAdmin) {
        Alert alert = authorizeAlert(alertId, userId, role, isAdmin);
        if (!Boolean.TRUE.equals(alert.getIsRead())) {
            alert.setIsRead(true);
            alert.setReadAt(LocalDateTime.now());
            if (alert.getStatus() == AlertStatus.NEW) {
                alert.setStatus(AlertStatus.READ);
            }
            alert = alertRepository.save(alert);
            publish("alert.read", toEvent(alert));
            log.info("Alert marked read alertId={} userId={}", alertId, userId);
        }
        return alertMapper.toResponse(alert);
    }

    @Transactional
    public void markAllAsRead(Long userId, String role) {
        List<Alert> alerts = alertRepository.findAll(myAlertsSpec(userId, role).and((root, query, cb) -> cb.isFalse(root.get("isRead"))));
        LocalDateTime now = LocalDateTime.now();
        alerts.forEach(alert -> {
            alert.setIsRead(true);
            alert.setReadAt(now);
            if (alert.getStatus() == AlertStatus.NEW) {
                alert.setStatus(AlertStatus.READ);
            }
        });
        alertRepository.saveAll(alerts);
        log.info("Marked all alerts read count={} userId={} role={}", alerts.size(), userId, role);
    }

    @Transactional
    public AlertResponse acknowledgeAlert(Long alertId, AcknowledgeAlertRequest request, Long userId, String role, boolean isAdmin) {
        Alert alert = authorizeAlert(alertId, userId, role, isAdmin);
        if (alert.getStatus() == AlertStatus.DISMISSED) {
            throw new InvalidAlertException("Dismissed alerts cannot be acknowledged");
        }
        if (!Boolean.TRUE.equals(alert.getIsRead())) {
            alert.setIsRead(true);
            alert.setReadAt(LocalDateTime.now());
        }
        alert.setIsAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(userId);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert = alertRepository.save(alert);
        publish("alert.acknowledged", toEvent(alert));
        log.info("Alert acknowledged alertId={} userId={}", alertId, userId);
        return alertMapper.toResponse(alert);
    }

    @Transactional
    public AlertResponse dismissAlert(Long alertId, DismissAlertRequest request, Long userId, String role, boolean isAdmin) {
        Alert alert = authorizeAlert(alertId, userId, role, isAdmin);
        alert.setIsDismissed(true);
        alert.setDismissedAt(LocalDateTime.now());
        alert.setDismissedBy(userId);
        alert.setStatus(AlertStatus.DISMISSED);
        alert = alertRepository.save(alert);
        publish("alert.dismissed", toEvent(alert));
        log.info("Alert dismissed alertId={} userId={}", alertId, userId);
        return alertMapper.toResponse(alert);
    }

    @Transactional
    public AlertResponse resolveAlert(Long alertId, Long actorId) {
        Alert alert = findAlert(alertId);
        alert.setStatus(AlertStatus.RESOLVED);
        alert = alertRepository.save(alert);
        publish("alert.resolved", toEvent(alert));
        log.info("Alert resolved alertId={} actorId={}", alertId, actorId);
        return alertMapper.toResponse(alert);
    }

    public long getUnreadCount(Long userId, String role) {
        return alertRepository.countByRecipientIdAndIsReadFalse(userId)
                + alertRepository.countByRecipientRoleAndIsReadFalse(role);
    }

    public AlertSummaryResponse getMyAlertSummary(Long userId, String role) {
        List<Alert> alerts = alertRepository.findAll(myAlertsSpec(userId, role));
        return buildSummary(alerts);
    }

    public AlertSummaryResponse getSystemAlertSummary() {
        return buildSummary(alertRepository.findAll());
    }

    public AlertAnalyticsResponse getAlertAnalytics(LocalDateTime fromDate, LocalDateTime toDate) {
        List<Alert> alerts = alertRepository.findAll(betweenDates(fromDate, toDate));
        return buildAnalytics(alerts);
    }

    @Transactional
    public void createAlertFromStockEvent(StockAlertEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }
        String eventType = event.getEventType().toUpperCase(Locale.ROOT);
        BigDecimal availableQuantity = firstNonNull(event.getAvailableQuantity(), event.getCurrentQuantity());
        if (eventType.contains("LOW")) {
            AlertSeverity severity = severityForLowStock(event);
            createRoleAlertFromEvent("MANAGER", AlertType.LOW_STOCK, severity, "Low Stock Alert",
                    String.format("Low stock detected for %s in %s. Available: %s, reorder level: %s.",
                            safe(event.getProductName()), safe(event.getWarehouseName()), safe(availableQuantity), safe(event.getReorderLevel())),
                    defaultSourceService(event.getSourceService(), "warehouse-service"), defaultCorrelationId(event.getCorrelationId(), eventType, event.getProductId(), event.getWarehouseId()), builder -> {
                        builder.relatedProductId(event.getProductId());
                        builder.relatedWarehouseId(event.getWarehouseId());
                        builder.referenceType(event.getReferenceType());
                        builder.referenceId(event.getReferenceId());
                        builder.referenceNumber(event.getReferenceNumber());
                        builder.actionUrl("/stocks/low-stock");
                    });
            if (severity == AlertSeverity.CRITICAL) {
                createRoleAlertFromEvent("ADMIN", AlertType.LOW_STOCK, AlertSeverity.CRITICAL, "Low Stock Alert",
                        String.format("Critical low stock detected for %s.", safe(event.getProductName())),
                        defaultSourceService(event.getSourceService(), "warehouse-service"), scopedCorrelationId(event.getCorrelationId(), eventType, event.getProductId(), event.getWarehouseId(), "ADMIN"), builder -> {
                            builder.relatedProductId(event.getProductId());
                            builder.relatedWarehouseId(event.getWarehouseId());
                            builder.actionUrl("/stocks/low-stock");
                        });
            }
        } else if (eventType.contains("OVER")) {
            createRoleAlertFromEvent("MANAGER", AlertType.OVERSTOCK, AlertSeverity.WARNING, "Overstock Alert",
                    String.format("Overstock detected for %s in %s. Current quantity: %s, max stock level: %s.",
                            safe(event.getProductName()), safe(event.getWarehouseName()), safe(availableQuantity), safe(event.getMaxStockLevel())),
                    defaultSourceService(event.getSourceService(), "warehouse-service"), defaultCorrelationId(event.getCorrelationId(), eventType, event.getProductId(), event.getWarehouseId()), builder -> {
                        builder.relatedProductId(event.getProductId());
                        builder.relatedWarehouseId(event.getWarehouseId());
                        builder.referenceType(event.getReferenceType());
                        builder.referenceId(event.getReferenceId());
                        builder.referenceNumber(event.getReferenceNumber());
                        builder.actionUrl("/stocks/overstock");
                    });
        } else if (eventType.contains("TRANSFER")) {
            createRoleAlertFromEvent("STAFF", AlertType.STOCK_TRANSFER, AlertSeverity.INFO, "Stock Transfer Completed",
                    String.format("Stock transfer completed for %s.", safe(event.getProductName())),
                    defaultSourceService(event.getSourceService(), "warehouse-service"), defaultCorrelationId(event.getCorrelationId(), eventType, event.getProductId(), event.getWarehouseId()), builder -> {
                        builder.relatedProductId(event.getProductId());
                        builder.relatedWarehouseId(event.getWarehouseId());
                        builder.actionUrl("/movements");
                    });
        }
    }

    @Transactional
    public void createAlertFromPurchaseEvent(PurchaseAlertEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }
        String eventType = event.getEventType().toUpperCase(Locale.ROOT);
        String sourceService = defaultSourceService(event.getSourceService(), "purchase-service");
        String baseCorrelationId = defaultCorrelationId(event.getCorrelationId(), eventType, event.getPurchaseOrderId(), event.getCreatedBy());
        if (eventType.contains("PENDING")) {
            createRoleAlertFromEvent("MANAGER", AlertType.PO_APPROVAL_PENDING, AlertSeverity.INFO,
                    "Purchase Order Pending Approval",
                    defaultMessage(event, String.format("Purchase order %s is pending approval.", safe(event.getPurchaseOrderNumber()))),
                    sourceService, scopedCorrelationId(baseCorrelationId, "MANAGER"), builder -> {
                        builder.relatedPurchaseOrderId(event.getPurchaseOrderId());
                        builder.referenceType("PURCHASE_ORDER");
                        builder.referenceId(stringify(event.getPurchaseOrderId()));
                        builder.referenceNumber(event.getPurchaseOrderNumber());
                        builder.actionUrl("/purchase-orders/approvals");
                    });
            createRoleAlertFromEvent("ADMIN", AlertType.PO_APPROVAL_PENDING, AlertSeverity.INFO,
                    "Purchase Order Pending Approval",
                    defaultMessage(event, String.format("Purchase order %s is pending approval.", safe(event.getPurchaseOrderNumber()))),
                    sourceService, scopedCorrelationId(baseCorrelationId, "ADMIN"), builder -> {
                        builder.relatedPurchaseOrderId(event.getPurchaseOrderId());
                        builder.referenceType("PURCHASE_ORDER");
                        builder.referenceId(stringify(event.getPurchaseOrderId()));
                        builder.referenceNumber(event.getPurchaseOrderNumber());
                        builder.actionUrl("/purchase-orders/approvals");
                    });
        } else if (eventType.contains("OVERDUE")) {
            for (String role : List.of("OFFICER", "MANAGER", "ADMIN")) {
                createRoleAlertFromEvent(role, AlertType.PO_OVERDUE_RECEIPT, AlertSeverity.CRITICAL,
                        "Overdue Purchase Order Receipt",
                        defaultMessage(event, String.format("Purchase order %s is overdue by %s day(s).",
                                safe(event.getPurchaseOrderNumber()), safe(event.getDaysOverdue()))),
                        sourceService, scopedCorrelationId(baseCorrelationId, role), builder -> {
                            builder.relatedPurchaseOrderId(event.getPurchaseOrderId());
                            builder.referenceType("PURCHASE_ORDER");
                            builder.referenceId(stringify(event.getPurchaseOrderId()));
                            builder.referenceNumber(event.getPurchaseOrderNumber());
                            builder.actionUrl("/purchase-orders/overdue");
                        });
            }
        } else if (eventType.contains("APPROVED")) {
            if (event.getCreatedBy() != null) {
                createUserAlertFromEvent(event.getCreatedBy(), AlertType.PO_APPROVED, AlertSeverity.INFO,
                        "Purchase Order Approved", defaultMessage(event, "Purchase order approved"),
                        sourceService, baseCorrelationId, builder -> {
                            builder.relatedPurchaseOrderId(event.getPurchaseOrderId());
                            builder.referenceType("PURCHASE_ORDER");
                            builder.referenceId(stringify(event.getPurchaseOrderId()));
                            builder.referenceNumber(event.getPurchaseOrderNumber());
                            builder.actionUrl("/purchase-orders/" + event.getPurchaseOrderId());
                        });
            }
        } else if (eventType.contains("REJECTED")) {
            if (event.getCreatedBy() != null) {
                createUserAlertFromEvent(event.getCreatedBy(), AlertType.PO_REJECTED, AlertSeverity.WARNING,
                        "Purchase Order Rejected", defaultMessage(event, "Purchase order rejected"),
                        sourceService, baseCorrelationId, builder -> {
                            builder.relatedPurchaseOrderId(event.getPurchaseOrderId());
                            builder.referenceType("PURCHASE_ORDER");
                            builder.referenceId(stringify(event.getPurchaseOrderId()));
                            builder.referenceNumber(event.getPurchaseOrderNumber());
                            builder.actionUrl("/purchase-orders/" + event.getPurchaseOrderId());
                        });
            }
        } else if (eventType.contains("RECEIVED")) {
            createRoleAlertFromEvent("OFFICER", AlertType.PO_RECEIVED, AlertSeverity.INFO,
                    "Purchase Order Received", defaultMessage(event, "Purchase order fully received"),
                    sourceService, scopedCorrelationId(baseCorrelationId, "OFFICER"), builder -> {
                        builder.relatedPurchaseOrderId(event.getPurchaseOrderId());
                        builder.referenceType("PURCHASE_ORDER");
                        builder.referenceId(stringify(event.getPurchaseOrderId()));
                        builder.referenceNumber(event.getPurchaseOrderNumber());
                        builder.actionUrl("/purchase-orders/" + event.getPurchaseOrderId());
                    });
        }
    }

    @Transactional
    public void createAlertFromSupplierEvent(SupplierAlertEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }
        String eventType = event.getEventType().toUpperCase(Locale.ROOT);
        String sourceService = defaultSourceService(event.getSourceService(), "supplier-service");
        String baseCorrelationId = defaultCorrelationId(event.getCorrelationId(), eventType, event.getSupplierId(), null);
        if (eventType.contains("BLACKLIST")) {
            for (String role : List.of("OFFICER", "ADMIN", "MANAGER")) {
                createRoleAlertFromEvent(role, AlertType.SUPPLIER_BLACKLISTED, AlertSeverity.CRITICAL,
                        "Supplier Blacklisted",
                        String.format("Supplier %s has been blacklisted. %s", safe(event.getSupplierName()), safe(event.getReason())),
                        sourceService, scopedCorrelationId(baseCorrelationId, role), builder -> {
                            builder.relatedSupplierId(event.getSupplierId());
                            builder.actionUrl("/suppliers/" + event.getSupplierId());
                        });
            }
        } else if (eventType.contains("DEACTIV")) {
            for (String role : List.of("OFFICER", "ADMIN", "MANAGER")) {
                createRoleAlertFromEvent(role, AlertType.SUPPLIER_DEACTIVATED, AlertSeverity.WARNING,
                        "Supplier Deactivated",
                        String.format("Supplier %s has been deactivated.", safe(event.getSupplierName())),
                        sourceService, scopedCorrelationId(baseCorrelationId, role), builder -> {
                            builder.relatedSupplierId(event.getSupplierId());
                            builder.actionUrl("/suppliers/" + event.getSupplierId());
                        });
            }
        }
    }

    @Transactional
    public void createAlertFromMovementEvent(MovementAlertEvent event) {
        if (event == null || !notBlank(event.getMessage())) {
            return;
        }
        for (String role : List.of("MANAGER", "ADMIN")) {
            createRoleAlertFromEvent(role, AlertType.MOVEMENT_ANOMALY, AlertSeverity.CRITICAL,
                    "Stock Movement Anomaly", safe(event.getMessage()),
                    defaultSourceService(event.getSourceService(), "movement-service"),
                    scopedCorrelationId(defaultCorrelationId(event.getCorrelationId(), event.getEventType(), event.getMovementId(), event.getProductId()), role), builder -> {
                        builder.relatedMovementId(event.getMovementId());
                        builder.relatedProductId(event.getProductId());
                        builder.relatedWarehouseId(event.getWarehouseId());
                        builder.actionUrl("/movements/" + event.getMovementId());
                    });
        }
    }

    @Transactional
    @Scheduled(cron = "${stockpro.alert.expiry-cron}")
    public void expireOldAlerts() {
        List<Alert> expired = alertRepository.findByExpiresAtBeforeAndStatusNot(LocalDateTime.now(), AlertStatus.EXPIRED);
        expired.forEach(alert -> alert.setStatus(AlertStatus.EXPIRED));
        if (!expired.isEmpty()) {
            alertRepository.saveAll(expired);
            log.info("Expired {} alerts", expired.size());
        }
    }

    private Alert buildAlert(CreateAlertRequest request) {
        return Alert.builder()
                .alertNumber(generateAlertNumber())
                .recipientId(request.getRecipientId())
                .recipientRole(normalize(request.getRecipientRole()))
                .type(request.getType())
                .severity(request.getSeverity())
                .status(AlertStatus.NEW)
                .channel(defaultChannel(request.getSeverity(), request.getChannel()))
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .relatedProductId(request.getRelatedProductId())
                .relatedWarehouseId(request.getRelatedWarehouseId())
                .relatedPurchaseOrderId(request.getRelatedPurchaseOrderId())
                .relatedSupplierId(request.getRelatedSupplierId())
                .relatedMovementId(request.getRelatedMovementId())
                .referenceType(normalize(request.getReferenceType()))
                .referenceId(normalize(request.getReferenceId()))
                .referenceNumber(normalize(request.getReferenceNumber()))
                .expiresAt(request.getExpiresAt())
                .sourceService("alert-service")
                .priority(priority(request.getSeverity()))
                .actionUrl(normalize(request.getActionUrl()))
                .metadataJson(normalize(request.getMetadataJson()))
                .build();
    }

    private Alert findAlert(Long alertId) {
        return alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found"));
    }

    private Alert authorizeAlert(Long alertId, Long userId, String role, boolean isAdmin) {
        return authorizeAlert(findAlert(alertId), userId, role, isAdmin);
    }

    private Alert authorizeAlert(Alert alert, Long userId, String role, boolean isAdmin) {
        if (isAdmin) {
            return alert;
        }
        boolean matchesUser = alert.getRecipientId() != null && Objects.equals(alert.getRecipientId(), userId);
        boolean matchesRole = alert.getRecipientRole() != null && alert.getRecipientRole().equalsIgnoreCase(role);
        if (!matchesUser && !matchesRole) {
            throw new org.springframework.security.access.AccessDeniedException("You are not allowed to access this alert");
        }
        return alert;
    }

    private Pageable pageRequest(int page, int size, String sortBy, String sortDir) {
        String sortField = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, sortField));
    }

    private Specification<Alert> myAlertsSpec(Long userId, String role) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("recipientId"), userId),
                cb.equal(cb.upper(root.get("recipientRole")), role.toUpperCase(Locale.ROOT))
        );
    }

    private Specification<Alert> searchSpec(AlertSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (notBlank(request.getKeyword())) {
                String like = "%" + request.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("alertNumber")), like),
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("message")), like),
                        cb.like(cb.lower(root.get("referenceNumber")), like)
                ));
            }
            if (request.getRecipientId() != null) predicates.add(cb.equal(root.get("recipientId"), request.getRecipientId()));
            if (notBlank(request.getRecipientRole())) predicates.add(cb.equal(cb.upper(root.get("recipientRole")), request.getRecipientRole().toUpperCase(Locale.ROOT)));
            if (request.getType() != null) predicates.add(cb.equal(root.get("type"), request.getType()));
            if (request.getSeverity() != null) predicates.add(cb.equal(root.get("severity"), request.getSeverity()));
            if (request.getStatus() != null) predicates.add(cb.equal(root.get("status"), request.getStatus()));
            if (request.getIsRead() != null) predicates.add(cb.equal(root.get("isRead"), request.getIsRead()));
            if (request.getIsAcknowledged() != null) predicates.add(cb.equal(root.get("isAcknowledged"), request.getIsAcknowledged()));
            if (request.getIsDismissed() != null) predicates.add(cb.equal(root.get("isDismissed"), request.getIsDismissed()));
            if (notBlank(request.getReferenceType())) predicates.add(cb.equal(root.get("referenceType"), request.getReferenceType()));
            if (notBlank(request.getReferenceId())) predicates.add(cb.equal(root.get("referenceId"), request.getReferenceId()));
            if (notBlank(request.getSourceService())) predicates.add(cb.equal(root.get("sourceService"), request.getSourceService()));
            if (request.getFromDate() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getFromDate()));
            if (request.getToDate() != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getToDate()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Alert> betweenDates(LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AlertSummaryResponse buildSummary(List<Alert> alerts) {
        return AlertSummaryResponse.builder()
                .totalAlerts(alerts.size())
                .unreadCount(alerts.stream().filter(alert -> !Boolean.TRUE.equals(alert.getIsRead())).count())
                .acknowledgedCount(alerts.stream().filter(alert -> Boolean.TRUE.equals(alert.getIsAcknowledged())).count())
                .dismissedCount(alerts.stream().filter(alert -> Boolean.TRUE.equals(alert.getIsDismissed())).count())
                .criticalCount(alerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL).count())
                .warningCount(alerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.WARNING).count())
                .infoCount(alerts.stream().filter(alert -> alert.getSeverity() == AlertSeverity.INFO).count())
                .lowStockCount(alerts.stream().filter(alert -> alert.getType() == AlertType.LOW_STOCK).count())
                .overstockCount(alerts.stream().filter(alert -> alert.getType() == AlertType.OVERSTOCK).count())
                .pendingPoApprovalCount(alerts.stream().filter(alert -> alert.getType() == AlertType.PO_APPROVAL_PENDING).count())
                .overduePoCount(alerts.stream().filter(alert -> alert.getType() == AlertType.PO_OVERDUE_RECEIPT).count())
                .build();
    }

    private AlertAnalyticsResponse buildAnalytics(List<Alert> alerts) {
        return AlertAnalyticsResponse.builder()
                .alertsByType(countMap(alerts.stream().collect(Collectors.groupingBy(alert -> alert.getType().name(), LinkedHashMap::new, Collectors.counting()))))
                .alertsBySeverity(countMap(alerts.stream().collect(Collectors.groupingBy(alert -> alert.getSeverity().name(), LinkedHashMap::new, Collectors.counting()))))
                .alertsByStatus(countMap(alerts.stream().collect(Collectors.groupingBy(alert -> alert.getStatus().name(), LinkedHashMap::new, Collectors.counting()))))
                .alertsByRole(countMap(alerts.stream().filter(alert -> alert.getRecipientRole() != null)
                        .collect(Collectors.groupingBy(Alert::getRecipientRole, LinkedHashMap::new, Collectors.counting()))))
                .dailyAlertTrend(countMap(alerts.stream().collect(Collectors.groupingBy(
                        alert -> alert.getCreatedAt().toLocalDate().toString(), LinkedHashMap::new, Collectors.counting()))))
                .topAlertedProducts(topCounts(alerts.stream().filter(alert -> alert.getRelatedProductId() != null)
                        .collect(Collectors.groupingBy(Alert::getRelatedProductId, Collectors.counting()))))
                .topAlertedWarehouses(topCounts(alerts.stream().filter(alert -> alert.getRelatedWarehouseId() != null)
                        .collect(Collectors.groupingBy(Alert::getRelatedWarehouseId, Collectors.counting()))))
                .build();
    }

    private Map<String, Long> countMap(Map<String, Long> source) {
        return new LinkedHashMap<>(source);
    }

    private List<AlertEntityCount> topCounts(Map<Long, Long> source) {
        return source.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> AlertEntityCount.builder().id(entry.getKey()).count(entry.getValue()).build())
                .toList();
    }

    private void createRoleAlertFromEvent(
            String recipientRole,
            AlertType type,
            AlertSeverity severity,
            String title,
            String message,
            String sourceService,
            String correlationId,
            java.util.function.Consumer<Alert.AlertBuilder> customizer) {
        if (notBlank(correlationId) && alertRepository.existsByCorrelationIdAndTypeAndRecipientRole(correlationId, type, recipientRole)) {
            log.info("Duplicate alert event skipped correlationId={} type={} recipientRole={}", correlationId, type, recipientRole);
            return;
        }
        Alert.AlertBuilder builder = Alert.builder()
                .alertNumber(generateAlertNumber())
                .recipientRole(recipientRole)
                .type(type)
                .severity(severity)
                .status(AlertStatus.NEW)
                .channel(defaultChannel(severity, null))
                .title(title)
                .message(message)
                .sourceService(sourceService != null ? sourceService : "unknown")
                .correlationId(correlationId)
                .priority(priority(severity));
        customizer.accept(builder);
        Alert saved = alertRepository.save(builder.build());
        publish("alert.created", toEvent(saved));
        maybeSendCriticalEmail(saved);
    }

    private void createUserAlertFromEvent(
            Long recipientId,
            AlertType type,
            AlertSeverity severity,
            String title,
            String message,
            String sourceService,
            String correlationId,
            java.util.function.Consumer<Alert.AlertBuilder> customizer) {
        if (notBlank(correlationId) && alertRepository.existsByCorrelationIdAndTypeAndRecipientId(correlationId, type, recipientId)) {
            log.info("Duplicate alert event skipped correlationId={} type={} recipientId={}", correlationId, type, recipientId);
            return;
        }
        Alert.AlertBuilder builder = Alert.builder()
                .alertNumber(generateAlertNumber())
                .recipientId(recipientId)
                .type(type)
                .severity(severity)
                .status(AlertStatus.NEW)
                .channel(defaultChannel(severity, null))
                .title(title)
                .message(message)
                .sourceService(sourceService != null ? sourceService : "unknown")
                .correlationId(correlationId)
                .priority(priority(severity));
        customizer.accept(builder);
        Alert saved = alertRepository.save(builder.build());
        publish("alert.created", toEvent(saved));
        maybeSendCriticalEmail(saved);
    }

    private String generateAlertNumber() {
        String prefix = "ALT-" + LocalDate.now().format(NUMBER_DATE) + "-";
        long next = alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(prefix)
                .map(Alert::getAlertNumber)
                .map(number -> number.substring(number.lastIndexOf('-') + 1))
                .map(Long::parseLong)
                .orElse(0L) + 1;
        return prefix + String.format("%06d", next);
    }

    private AlertSeverity severityForLowStock(StockAlertEvent event) {
        BigDecimal availableQuantity = firstNonNull(event.getAvailableQuantity(), event.getCurrentQuantity());
        if (availableQuantity != null && event.getReorderLevel() != null
                && event.getReorderLevel().signum() > 0
                && availableQuantity.compareTo(event.getReorderLevel().divide(BigDecimal.valueOf(2), java.math.RoundingMode.HALF_UP)) <= 0) {
            return AlertSeverity.CRITICAL;
        }
        return AlertSeverity.WARNING;
    }

    private void maybeSendCriticalEmail(Alert alert) {
        if (alert.getSeverity() == AlertSeverity.CRITICAL) {
            boolean sent = emailNotificationService.sendCriticalAlertEmail(alert, null);
            publish(sent ? "alert.email.sent" : "alert.email.failed", toEvent(alert));
        }
    }

    private AlertEvent toEvent(Alert alert) {
        return AlertEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .alertId(alert.getAlertId())
                .alertNumber(alert.getAlertNumber())
                .recipientId(alert.getRecipientId())
                .recipientRole(alert.getRecipientRole())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .channel(alert.getChannel())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .referenceType(alert.getReferenceType())
                .referenceId(alert.getReferenceId())
                .sourceService(alert.getSourceService())
                .correlationId(alert.getCorrelationId())
                .createdAt(alert.getCreatedAt())
                .build();
    }

    private void publish(String routingKey, AlertEvent event) {
        try {
            alertEventPublisher.publish(routingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish alert event routingKey={} alertId={}", routingKey, event.getAlertId(), ex);
        }
    }

    private int priority(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 1;
            case WARNING -> 2;
            case INFO -> 3;
        };
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private String defaultMessage(PurchaseAlertEvent event, String fallback) {
        return notBlank(event.getMessage()) ? event.getMessage() : fallback;
    }

    private AlertChannel defaultChannel(AlertSeverity severity, AlertChannel requestedChannel) {
        if (severity == AlertSeverity.CRITICAL) {
            return AlertChannel.BOTH;
        }
        return requestedChannel != null ? requestedChannel : AlertChannel.IN_APP;
    }

    private String defaultSourceService(String sourceService, String fallback) {
        return notBlank(sourceService) ? sourceService : fallback;
    }

    private String defaultCorrelationId(String correlationId, String eventType, Object primaryRef, Object secondaryRef) {
        if (notBlank(correlationId)) {
            return correlationId;
        }
        return String.join(":", safe(eventType), safe(primaryRef), safe(secondaryRef));
    }

    private String scopedCorrelationId(String correlationId, String scope) {
        return correlationId + ":" + scope;
    }

    private String scopedCorrelationId(String correlationId, String eventType, Object primaryRef, Object secondaryRef, String scope) {
        return defaultCorrelationId(correlationId, eventType, primaryRef, secondaryRef) + ":" + scope;
    }

    private BigDecimal firstNonNull(BigDecimal preferred, BigDecimal fallback) {
        return preferred != null ? preferred : fallback;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
