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
import com.stockpro.alertservice.events.PaymentAlertEvent;
import com.stockpro.alertservice.events.PurchaseAlertEvent;
import com.stockpro.alertservice.events.StockAlertEvent;
import com.stockpro.alertservice.events.SupplierAlertEvent;
import com.stockpro.alertservice.events.SystemAlertEvent;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.EnumSet;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "INVENTORY_MANAGER";
    private static final String ROLE_OFFICER = "PURCHASE_OFFICER";
    private static final String ROLE_STAFF = "WAREHOUSE_STAFF";
    private static final String ROLE_MANAGER_ALIAS = "MANAGER";
    private static final String ROLE_OFFICER_ALIAS = "OFFICER";
    private static final String ROLE_STAFF_ALIAS = "STAFF";
    private static final String ROLE_ALL = "ALL";
    private static final String FIELD_IS_READ = "isRead";
    private static final String FIELD_RECIPIENT_ROLE = "recipientRole";
    private static final List<String> BROADCAST_ROLES = List.of(ROLE_ADMIN, ROLE_MANAGER, ROLE_OFFICER, ROLE_STAFF);
    private static final EnumSet<AlertType> USER_VISIBLE_ALERT_TYPES = EnumSet.of(
            AlertType.LOW_STOCK,
            AlertType.OVERSTOCK,
            AlertType.PO_APPROVAL_PENDING,
            AlertType.OVERDUE_RECEIPT,
            AlertType.SYSTEM_BROADCAST);
    private static final EnumSet<AlertType> ARCHIVED_NON_BUSINESS_ALERT_TYPES = EnumSet.of(
            AlertType.SYSTEM_ERROR,
            AlertType.UNAUTHORIZED_ACCESS,
            AlertType.STOCK_UPDATED,
            AlertType.PO_CREATED,
            AlertType.PO_SUBMITTED,
            AlertType.PO_APPROVED,
            AlertType.PO_REJECTED,
            AlertType.PO_CANCELLED,
            AlertType.PO_RECEIVED,
            AlertType.GRN_STARTED,
            AlertType.GRN_PARTIAL,
            AlertType.GRN_COMPLETED,
            AlertType.SUPPLIER_DEACTIVATED,
            AlertType.SUPPLIER_BLACKLISTED,
            AlertType.MOVEMENT_ANOMALY,
            AlertType.STOCK_TRANSFER,
            AlertType.WAREHOUSE_TRANSFER_INITIATED,
            AlertType.WAREHOUSE_TRANSFER_COMPLETED,
            AlertType.REPORT_READY,
            AlertType.PAYMENT_PENDING,
            AlertType.PAYMENT_INITIATED,
            AlertType.PAYMENT_SUCCESSFUL,
            AlertType.PAYMENT_FAILED,
            AlertType.PAYMENT_CANCELLED,
            AlertType.PAYMENT_LIMIT_EXCEEDED,
            AlertType.SPLIT_PAYMENT_RECOMMENDED,
            AlertType.PAYMENT_COMPLETED,
            AlertType.GENERAL);
    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_RECEIVED = "RECEIVED";
    private static final String STATUS_FULLY_RECEIVED = "FULLY_RECEIVED";
    private static final String STATUS_PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_UNKNOWN = "unknown";
    private static final String EVENT_SUBMITTED = "SUBMITTED";
    private static final String EVENT_PENDING = "PENDING";
    private static final String EVENT_OVERDUE = "OVERDUE";
    private static final String EVENT_LOW = "LOW";
    private static final String EVENT_OVER = "OVER";
    private static final String SOURCE_ALERT_SERVICE = "alert-service";
    private static final String REFERENCE_PURCHASE_ORDER = "PURCHASE_ORDER";
    private static final int OUT_OF_STOCK_QUANTITY = 0;

    private final AlertRepository alertRepository;
    private final AlertValidationService validationService;
    private final AlertMapper alertMapper;
    private final AlertEventPublisher alertEventPublisher;
    private final EmailNotificationService emailNotificationService;

    @Value("${stockpro.alert.overdue-critical-days:3}")
    private int overdueCriticalDays;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void alignUserVisibleAlerts() {
        int migratedOverdueAlerts = alertRepository.replaceAlertType(AlertType.PO_OVERDUE_RECEIPT, AlertType.OVERDUE_RECEIPT);
        int normalizedManagerRoles = alertRepository.replaceRecipientRole(ROLE_MANAGER_ALIAS, ROLE_MANAGER);
        int normalizedOfficerRoles = alertRepository.replaceRecipientRole(ROLE_OFFICER_ALIAS, ROLE_OFFICER);
        int normalizedStaffRoles = alertRepository.replaceRecipientRole(ROLE_STAFF_ALIAS, ROLE_STAFF);
        int archivedAlerts = alertRepository.archiveByTypes(List.copyOf(ARCHIVED_NON_BUSINESS_ALERT_TYPES));
        int unarchivedBroadcastAlerts = alertRepository.unarchiveByTypes(List.of(AlertType.SYSTEM_BROADCAST));
        if (migratedOverdueAlerts > 0
                || normalizedManagerRoles > 0
                || normalizedOfficerRoles > 0
                || normalizedStaffRoles > 0
                || archivedAlerts > 0
                || unarchivedBroadcastAlerts > 0) {
            log.info(
                    "Aligned alert visibility model migratedOverdueAlerts={} normalizedManagerRoles={} normalizedOfficerRoles={} normalizedStaffRoles={} archivedAlerts={} unarchivedBroadcastAlerts={}",
                    migratedOverdueAlerts,
                    normalizedManagerRoles,
                    normalizedOfficerRoles,
                    normalizedStaffRoles,
                    archivedAlerts,
                    unarchivedBroadcastAlerts);
        }
    }

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
        List<String> recipientRoles = resolveBroadcastRoles(request);

        log.info("Preparing broadcast alert title={} severity={} requestedRoles={} normalizedRoles={} type={} recipientIds={}",
                request.getTitle(),
                request.getSeverity(),
                request.resolveRecipientRoles(),
                recipientRoles,
                AlertType.SYSTEM_BROADCAST,
                request.getRecipientIds());

        if (!recipientRoles.isEmpty()) {
            for (String role : recipientRoles) {
                log.info("Creating broadcast alert for normalizedRecipientRole={} actorId={}", role, actorId);
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

        log.info("Broadcast alert created count={} actorId={} savedRecipientRoles={}",
                responses.size(), actorId, responses.stream().map(AlertResponse::getRecipientRole).toList());
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
        Specification<Alert> spec = myAlertsSpec(userId, role)
                .and(userVisibleAlertSpec())
                .and(searchSpec(request));
        return alertRepository.findAll(spec, pageable).map(alertMapper::toResponse);
    }

    public Page<AlertResponse> searchAlerts(AlertSearchRequest request, Long userId, String role, boolean isAdmin) {
        Pageable pageable = pageRequest(valueOrDefault(request.getPage(), 0), valueOrDefault(request.getSize(), 10),
                request.getSortBy(), request.getSortDir());
        Specification<Alert> spec = userVisibleAlertSpec().and(searchSpec(request));
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
        List<Alert> alerts = alertRepository.findAll(myAlertsSpec(userId, role)
                .and(userVisibleAlertSpec())
                .and((root, query, cb) -> cb.isFalse(root.get(FIELD_IS_READ))));
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
        if (Boolean.TRUE.equals(alert.getIsAcknowledged()) || alert.getStatus() == AlertStatus.ACKNOWLEDGED) {
            return alertMapper.toResponse(alert);
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
        if (Boolean.TRUE.equals(alert.getIsDismissed()) || alert.getStatus() == AlertStatus.DISMISSED) {
            return alertMapper.toResponse(alert);
        }
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
        return alertRepository.count(myAlertsSpec(userId, role)
                .and(userVisibleAlertSpec())
                .and((root, query, cb) -> cb.isFalse(root.get(FIELD_IS_READ))));
    }

    public AlertSummaryResponse getMyAlertSummary(Long userId, String role) {
        List<Alert> alerts = alertRepository.findAll(myAlertsSpec(userId, role).and(userVisibleAlertSpec()));
        return buildSummary(alerts);
    }

    public AlertSummaryResponse getSystemAlertSummary() {
        return buildSummary(alertRepository.findAll(userVisibleAlertSpec()));
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
        String sourceService = defaultSourceService(event.getSourceService(), "warehouse-service");
        String correlationId = defaultCorrelationId(event.getCorrelationId(), eventType, event.getProductId(), event.getWarehouseId());
        BigDecimal availableQuantity = firstNonNull(event.getAvailableQuantity(), event.getCurrentQuantity());
        if (eventType.contains(EVENT_LOW)) {
            handleLowStockEvent(event, sourceService, correlationId, availableQuantity, eventType);
            return;
        }
        if (eventType.contains(EVENT_OVER)) {
            handleOverstockEvent(event, sourceService, correlationId, availableQuantity);
            return;
        }
        log.debug("Ignoring non case-study stock alert eventType={}", eventType);
    }

    @Transactional
    public void createAlertFromPurchaseEvent(PurchaseAlertEvent event) {
        if (event == null || !hasPurchaseAlertSignal(event)) {
            return;
        }
        String eventType = normalizedPurchaseEventType(event);
        String sourceService = defaultSourceService(event.getSourceService(), "purchase-service");
        String baseCorrelationId = defaultCorrelationId(event.getCorrelationId(), eventType, event.getPurchaseOrderId(), event.getCreatedBy());
        if (eventType.contains(EVENT_SUBMITTED) || eventType.contains(EVENT_PENDING)) {
            handleSubmittedOrPendingPurchaseEvent(event, sourceService, baseCorrelationId);
            return;
        }
        if (eventType.contains(EVENT_OVERDUE)) {
            handleOverduePurchaseEvent(event, sourceService, baseCorrelationId);
            return;
        }
        log.debug("Ignoring non case-study purchase alert eventType={}", eventType);
    }

    @Transactional
    public void createAlertFromSupplierEvent(SupplierAlertEvent event) {
        log.debug("Ignoring supplier alert event because supplier alerts are not user-facing in this case study. eventType={}",
                event != null ? event.getEventType() : null);
    }

    @Transactional
    public void createAlertFromMovementEvent(MovementAlertEvent event) {
        log.debug("Ignoring movement alert event because movement anomaly alerts are not user-facing in this case study. eventType={}",
                event != null ? event.getEventType() : null);
    }

    @Transactional
    public void createAlertFromPaymentEvent(PaymentAlertEvent event) {
        log.debug("Ignoring payment alert event because payment alerts are not user-facing in this case study. eventType={}",
                event != null ? event.getEventType() : null);
    }

    @Transactional
    public void createAlertFromSystemEvent(SystemAlertEvent event) {
        log.warn("Ignoring technical/system alert event from sourceService={} eventType={} because technical alerts are log-only.",
                event != null ? event.getSourceService() : null,
                event != null ? event.getEventType() : null);
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
        String cleanMessage = sanitizedMessageFor(request.getType(), request.getMessage(), request.getUserMessage());
        String sanitizedTechnicalDetails = sanitizeTechnicalDetails(
                firstNonBlank(request.getTechnicalDetails(), request.getMessage()),
                request.getType());
        return Alert.builder()
                .alertNumber(generateAlertNumber())
                .recipientId(request.getRecipientId())
                .recipientRole(notBlank(request.getRecipientRole()) ? canonicalRole(request.getRecipientRole()) : null)
                .type(request.getType())
                .severity(request.getSeverity())
                .status(AlertStatus.NEW)
                .channel(defaultChannel(request.getSeverity(), request.getChannel()))
                .title(request.getTitle().trim())
                .message(cleanMessage)
                .userMessage(cleanMessage)
                .technicalDetails(sanitizedTechnicalDetails)
                .relatedProductId(request.getRelatedProductId())
                .relatedWarehouseId(request.getRelatedWarehouseId())
                .relatedPurchaseOrderId(request.getRelatedPurchaseOrderId())
                .relatedSupplierId(request.getRelatedSupplierId())
                .relatedMovementId(request.getRelatedMovementId())
                .referenceType(normalize(request.getReferenceType()))
                .referenceId(normalize(request.getReferenceId()))
                .referenceNumber(normalize(request.getReferenceNumber()))
                .expiresAt(request.getExpiresAt())
                .sourceService(SOURCE_ALERT_SERVICE)
                .priority(priority(request.getSeverity()))
                .actionUrl(normalize(request.getActionUrl()))
                .metadataJson(normalize(request.getMetadataJson()))
                .isArchived(false)
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
        boolean matchesRole = matchesRole(alert.getRecipientRole(), role);
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
        List<String> roleVariants = canonicalRoleVariants(role);
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("recipientId"), userId),
                cb.upper(root.get(FIELD_RECIPIENT_ROLE)).in(roleVariants),
                cb.equal(cb.upper(root.get(FIELD_RECIPIENT_ROLE)), ROLE_ALL)
        );
    }

    private Specification<Alert> userVisibleAlertSpec() {
        return (root, query, cb) -> cb.and(
                cb.or(cb.isFalse(root.get("isArchived")), cb.isNull(root.get("isArchived"))),
                root.get("type").in(USER_VISIBLE_ALERT_TYPES));
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
            if (notBlank(request.getRecipientRole())) {
                predicates.add(cb.upper(root.get(FIELD_RECIPIENT_ROLE)).in(canonicalRoleVariants(request.getRecipientRole())));
            }
            if (request.getType() != null) predicates.add(cb.equal(root.get("type"), request.getType()));
            if (request.getSeverity() != null) predicates.add(cb.equal(root.get("severity"), request.getSeverity()));
            if (request.getStatus() != null) predicates.add(cb.equal(root.get("status"), request.getStatus()));
            if (request.getIsRead() != null) predicates.add(cb.equal(root.get(FIELD_IS_READ), request.getIsRead()));
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
                .overduePoCount(alerts.stream().filter(alert -> alert.getType() == AlertType.OVERDUE_RECEIPT).count())
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
            AlertDispatch dispatch,
            java.util.function.Consumer<Alert.AlertBuilder> customizer) {
        if (notBlank(dispatch.correlationId())
                && alertRepository.existsByCorrelationIdAndTypeAndRecipientRole(dispatch.correlationId(), dispatch.type(), recipientRole)) {
            log.info("Duplicate alert event skipped correlationId={} type={} recipientRole={}",
                    dispatch.correlationId(), dispatch.type(), recipientRole);
            return;
        }
        Alert.AlertBuilder builder = Alert.builder()
                .alertNumber(generateAlertNumber())
                .recipientRole(recipientRole)
                .type(dispatch.type())
                .severity(dispatch.severity())
                .status(AlertStatus.NEW)
                .channel(defaultChannel(dispatch.severity(), null))
                .title(dispatch.title())
                .message(dispatch.message())
                .sourceService(dispatch.sourceService() != null ? dispatch.sourceService() : STATUS_UNKNOWN)
                .correlationId(dispatch.correlationId())
                .priority(priority(dispatch.severity()));
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
        if (availableQuantity != null && availableQuantity.compareTo(BigDecimal.valueOf(OUT_OF_STOCK_QUANTITY)) <= 0) {
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

    private void createPurchaseRoleAlert(
            PurchaseAlertEvent event,
            String role,
            AlertDispatch dispatch) {
        createPurchaseRoleAlert(event, role, dispatch, purchaseOrderActionUrl(event));
    }

    private void createPurchaseRoleAlert(
            PurchaseAlertEvent event,
            String role,
            AlertDispatch dispatch,
            String actionUrl) {
        createRoleAlertFromEvent(role, dispatch,
                builder -> enrichPurchaseAlert(builder, event, actionUrl));
    }

    private void enrichPurchaseAlert(Alert.AlertBuilder builder, PurchaseAlertEvent event, String actionUrl) {
        builder.relatedPurchaseOrderId(event.getPurchaseOrderId());
        builder.referenceType(REFERENCE_PURCHASE_ORDER);
        builder.referenceId(event.getPurchaseOrderId() == null ? null : String.valueOf(event.getPurchaseOrderId()));
        builder.referenceNumber(event.getPurchaseOrderNumber());
        builder.actionUrl(actionUrl);
    }

    private boolean hasPurchaseAlertSignal(PurchaseAlertEvent event) {
        return event.getEventType() != null
                || notBlank(event.getNewStatus())
                || notBlank(event.getStatus())
                || notBlank(event.getOldStatus());
    }

    private String normalizedPurchaseEventType(PurchaseAlertEvent event) {
        if (notBlank(event.getEventType())) {
            return event.getEventType().toUpperCase(Locale.ROOT);
        }
        if (matchesPurchaseStatus(event, STATUS_PENDING_PAYMENT)) {
            return "PURCHASE_ORDER_UPDATED";
        }
        if (matchesPurchaseStatus(event, STATUS_PENDING_APPROVAL)) {
            return "PURCHASE_ORDER_PENDING_APPROVAL";
        }
        if (matchesPurchaseStatus(event, STATUS_APPROVED)) {
            return "PURCHASE_ORDER_APPROVED";
        }
        if (matchesPurchaseStatus(event, STATUS_REJECTED)) {
            return "PURCHASE_ORDER_REJECTED";
        }
        if (matchesAnyPurchaseStatus(event, STATUS_RECEIVED, STATUS_FULLY_RECEIVED)) {
            return "PURCHASE_ORDER_RECEIVED";
        }
        if (matchesPurchaseStatus(event, STATUS_PARTIALLY_RECEIVED)) {
            return "PURCHASE_ORDER_PARTIALLY_RECEIVED";
        }
        if (matchesPurchaseStatus(event, STATUS_CANCELLED)) {
            return "PURCHASE_ORDER_CANCELLED";
        }
        return safe(event.getEventType()).toUpperCase(Locale.ROOT);
    }

    private boolean matchesPurchaseStatus(PurchaseAlertEvent event, String status) {
        return status.equalsIgnoreCase(event.getNewStatus()) || status.equalsIgnoreCase(event.getStatus());
    }

    private boolean matchesAnyPurchaseStatus(PurchaseAlertEvent event, String firstStatus, String secondStatus) {
        return matchesPurchaseStatus(event, firstStatus) || matchesPurchaseStatus(event, secondStatus);
    }

    private void handleLowStockEvent(StockAlertEvent event, String sourceService, String correlationId,
                                     BigDecimal availableQuantity, String eventType) {
        AlertSeverity severity = severityForLowStock(event);
        createRoleAlertFromEvent(ROLE_MANAGER, dispatch(
                AlertType.LOW_STOCK,
                severity,
                "Low Stock Alert",
                String.format("Product stock has fallen below reorder level for %s in %s. Current quantity: %s, reorder level: %s.",
                        safe(event.getProductName()), safe(event.getWarehouseName()), safe(availableQuantity), safe(event.getReorderLevel())),
                sourceService,
                correlationId), builder -> {
                    builder.relatedProductId(event.getProductId());
                    builder.relatedWarehouseId(event.getWarehouseId());
                    builder.referenceType(event.getReferenceType());
                    builder.referenceId(event.getReferenceId());
                    builder.referenceNumber(event.getReferenceNumber());
                    builder.actionUrl("/stocks/low-stock");
                });
        if (severity == AlertSeverity.CRITICAL) {
            createRoleAlertFromEvent(ROLE_ADMIN, dispatch(
                    AlertType.LOW_STOCK,
                    AlertSeverity.CRITICAL,
                    "Low Stock Alert",
                    String.format("Critical low stock detected for %s in %s. Immediate replenishment is recommended.",
                            safe(event.getProductName()), safe(event.getWarehouseName())),
                    sourceService,
                    scopedCorrelationId(event.getCorrelationId(), eventType, event.getProductId(), event.getWarehouseId(), ROLE_ADMIN)), builder -> {
                        builder.relatedProductId(event.getProductId());
                        builder.relatedWarehouseId(event.getWarehouseId());
                        builder.actionUrl("/stocks/low-stock");
                    });
        }
    }

    private void handleOverstockEvent(StockAlertEvent event, String sourceService, String correlationId, BigDecimal availableQuantity) {
        createRoleAlertFromEvent(ROLE_MANAGER, dispatch(
                AlertType.OVERSTOCK,
                AlertSeverity.WARNING,
                "Overstock Alert",
                String.format("Product stock exceeds the recommended maximum for %s in %s. Current quantity: %s, max stock level: %s.",
                        safe(event.getProductName()), safe(event.getWarehouseName()), safe(availableQuantity), safe(event.getMaxStockLevel())),
                sourceService,
                correlationId), builder -> {
                    builder.relatedProductId(event.getProductId());
                    builder.relatedWarehouseId(event.getWarehouseId());
                    builder.referenceType(event.getReferenceType());
                    builder.referenceId(event.getReferenceId());
                    builder.referenceNumber(event.getReferenceNumber());
                    builder.actionUrl("/stocks/overstock");
                });
    }

    private void handleSubmittedOrPendingPurchaseEvent(PurchaseAlertEvent event, String sourceService, String baseCorrelationId) {
        AlertDispatch dispatch = dispatch(
                AlertType.PO_APPROVAL_PENDING,
                AlertSeverity.INFO,
                "Purchase Order Pending Approval",
                defaultMessage(event, String.format("Purchase order %s from the purchase officer is waiting for approval.",
                        safe(event.getPurchaseOrderNumber()))),
                sourceService,
                null);
        createPurchaseRoleAlert(event, ROLE_MANAGER, dispatch.withCorrelationId(scopedCorrelationId(baseCorrelationId, ROLE_MANAGER)));
        createPurchaseRoleAlert(event, ROLE_ADMIN, dispatch.withCorrelationId(scopedCorrelationId(baseCorrelationId, ROLE_ADMIN)));
    }

    private void handleOverduePurchaseEvent(PurchaseAlertEvent event, String sourceService, String baseCorrelationId) {
        AlertSeverity severity = resolveOverdueSeverity(event.getDaysOverdue());
        for (String role : List.of(ROLE_STAFF, ROLE_MANAGER, ROLE_ADMIN)) {
            createPurchaseRoleAlert(event, role, dispatch(
                    AlertType.OVERDUE_RECEIPT,
                    severity,
                    "Overdue Receipt Alert",
                    defaultMessage(event, String.format("PO %s expected delivery date has passed without GRN. Overdue by %s day(s).",
                            safe(event.getPurchaseOrderNumber()), safe(event.getDaysOverdue()))),
                    sourceService,
                    scopedCorrelationId(baseCorrelationId, role)), "/purchase-orders/receiving");
        }
    }

    private AlertSeverity resolveOverdueSeverity(Integer daysOverdue) {
        return daysOverdue != null && daysOverdue >= overdueCriticalDays
                ? AlertSeverity.CRITICAL
                : AlertSeverity.WARNING;
    }

    private AlertDispatch dispatch(
            AlertType type,
            AlertSeverity severity,
            String title,
            String message,
            String sourceService,
            String correlationId) {
        return new AlertDispatch(type, severity, title, message, sourceService, correlationId);
    }

    private String purchaseOrderActionUrl(PurchaseAlertEvent event) {
        return event.getPurchaseOrderId() != null
                ? "/purchase-orders/" + event.getPurchaseOrderId()
                : "/purchase-orders/approvals";
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

    private String firstNonBlank(String preferred, String fallback) {
        return notBlank(preferred) ? preferred : fallback;
    }

    private String sanitizedMessageFor(AlertType type, String message, String userMessage) {
        String preferred = firstNonBlank(userMessage, message);
        if (type == AlertType.SYSTEM_ERROR) {
            return sanitizeUserMessage(preferred);
        }
        return normalize(preferred);
    }

    private String sanitizeUserMessage(String value) {
        if (!notBlank(value)) {
            return "A system operation failed. Please try again or contact admin.";
        }
        String normalized = value.trim();
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.contains("movement") && lowered.contains("revers")) {
            return "Movement reversal failed. Please try again or contact admin.";
        }
        if (lowered.contains("could not execute statement")
                || lowered.contains("data truncated")
                || lowered.contains("jdbc")
                || lowered.contains("sql")
                || lowered.contains("stack")
                || lowered.contains("exception")) {
            return "A system operation failed. Please try again or contact admin.";
        }
        return truncate(normalized, 2000);
    }

    private String sanitizeTechnicalDetails(String value, AlertType type) {
        if (!notBlank(value) || type != AlertType.SYSTEM_ERROR) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("select ") || normalized.contains("insert ") || normalized.contains("update ")
                || normalized.contains("delete ") || normalized.contains("jdbc") || normalized.contains("sql")
                || normalized.contains("stack") || normalized.contains("exception")) {
            return "System error details were captured in backend logs for further investigation.";
        }
        return truncate(value.trim(), 2000);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private List<String> resolveBroadcastRoles(CreateBroadcastAlertRequest request) {
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (String role : request.resolveRecipientRoles()) {
            String canonicalRole = canonicalRole(role);
            if (ROLE_ALL.equals(canonicalRole)) {
                resolved.addAll(BROADCAST_ROLES);
            } else if (canonicalRole != null) {
                resolved.add(canonicalRole);
            }
        }
        return new ArrayList<>(resolved);
    }

    private String canonicalRole(String role) {
        if (!notBlank(role)) {
            return null;
        }
        return switch (role.trim().toUpperCase(Locale.ROOT)) {
            case ROLE_ALL -> ROLE_ALL;
            case ROLE_ADMIN -> ROLE_ADMIN;
            case ROLE_MANAGER, ROLE_MANAGER_ALIAS -> ROLE_MANAGER;
            case ROLE_OFFICER, ROLE_OFFICER_ALIAS -> ROLE_OFFICER;
            case ROLE_STAFF, ROLE_STAFF_ALIAS -> ROLE_STAFF;
            default -> throw new IllegalArgumentException("Unsupported target role: " + role);
        };
    }

    private List<String> canonicalRoleVariants(String role) {
        String canonicalRole = canonicalRole(role);
        if (!notBlank(canonicalRole)) {
            return List.of();
        }
        if (ROLE_ALL.equals(canonicalRole) || ROLE_ADMIN.equals(canonicalRole)) {
            return List.of(canonicalRole);
        }
        if (ROLE_MANAGER.equals(canonicalRole)) {
            return List.of(ROLE_MANAGER, ROLE_MANAGER_ALIAS);
        }
        if (ROLE_OFFICER.equals(canonicalRole)) {
            return List.of(ROLE_OFFICER, ROLE_OFFICER_ALIAS);
        }
        if (ROLE_STAFF.equals(canonicalRole)) {
            return List.of(ROLE_STAFF, ROLE_STAFF_ALIAS);
        }
        return List.of(canonicalRole);
    }

    private boolean matchesRole(String alertRecipientRole, String authenticatedRole) {
        if (!notBlank(alertRecipientRole) || !notBlank(authenticatedRole)) {
            return false;
        }
        return canonicalRoleVariants(authenticatedRole).contains(alertRecipientRole.trim().toUpperCase(Locale.ROOT))
                || ROLE_ALL.equalsIgnoreCase(alertRecipientRole);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private record AlertDispatch(
            AlertType type,
            AlertSeverity severity,
            String title,
            String message,
            String sourceService,
            String correlationId) {

        private AlertDispatch withCorrelationId(String updatedCorrelationId) {
            return new AlertDispatch(type, severity, title, message, sourceService, updatedCorrelationId);
        }
    }
}
