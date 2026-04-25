package com.stockpro.alert.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.alert.client.AuthServiceClient;
import com.stockpro.alert.dto.event.EmailAlertEvent;
import com.stockpro.alert.dto.event.LowStockEvent;
import com.stockpro.alert.dto.event.OverdueReceiptEvent;
import com.stockpro.alert.dto.event.OverstockEvent;
import com.stockpro.alert.dto.event.PoPendingApprovalEvent;
import com.stockpro.alert.dto.event.SystemAlertEvent;
import com.stockpro.alert.dto.request.AlertSearchRequest;
import com.stockpro.alert.dto.request.SendAlertRequest;
import com.stockpro.alert.dto.request.SendBulkAlertRequest;
import com.stockpro.alert.dto.response.AlertResponse;
import com.stockpro.alert.dto.response.AuthUserResponse;
import com.stockpro.alert.dto.response.UnreadCountResponse;
import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import com.stockpro.alert.exception.AlertNotFoundException;
import com.stockpro.alert.exception.InvalidAlertRequestException;
import com.stockpro.alert.exception.UnauthorizedAlertAccessException;
import com.stockpro.alert.mapper.AlertMapper;
import com.stockpro.alert.publisher.AlertEventPublisher;
import com.stockpro.alert.repository.AlertRepository;
import com.stockpro.alert.repository.AlertSpecifications;
import com.stockpro.alert.security.AuthenticatedUser;
import com.stockpro.alert.security.SecurityUtils;
import com.stockpro.alert.service.AlertService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AlertServiceImpl implements AlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> SORT_FIELDS = Set.of(
            "alertId",
            "recipientId",
            "type",
            "severity",
            "title",
            "channel",
            "isRead",
            "isAcknowledged",
            "createdAt",
            "readAt",
            "acknowledgedAt");

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final SecurityUtils securityUtils;
    private final AlertEventPublisher alertEventPublisher;
    private final AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${alert.email.lookup-enabled:true}")
    private boolean emailLookupEnabled;

    public AlertServiceImpl(AlertRepository alertRepository,
            AlertMapper alertMapper,
            SecurityUtils securityUtils,
            AlertEventPublisher alertEventPublisher,
            AuthServiceClient authServiceClient,
            ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
        this.securityUtils = securityUtils;
        this.alertEventPublisher = alertEventPublisher;
        this.authServiceClient = authServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AlertResponse sendAlert(SendAlertRequest request) {
        Alert alert = Alert.builder()
                .recipientId(normalizeRequiredId(request.getRecipientId(), "recipientId is required."))
                .type(requireType(request.getType()))
                .severity(requireSeverity(request.getSeverity()))
                .title(normalizeRequiredText(request.getTitle(), "title is required."))
                .message(normalizeRequiredText(request.getMessage(), "message is required."))
                .relatedProductId(normalizeOptionalId(request.getRelatedProductId()))
                .relatedWarehouseId(normalizeOptionalId(request.getRelatedWarehouseId()))
                .relatedPurchaseOrderId(normalizeOptionalId(request.getRelatedPurchaseOrderId()))
                .channel(resolveEffectiveChannel(request.getChannel(), request.getSeverity()))
                .metadata(toMetadata(Map.of("source", "API")))
                .build();

        return alertMapper.toResponse(saveAlert(alert));
    }

    @Override
    public List<AlertResponse> sendBulk(SendBulkAlertRequest request) {
        List<AlertResponse> responses = new ArrayList<>();
        request.getRecipientIds().stream()
                .distinct()
                .forEach(recipientId -> responses.add(sendAlert(toSingleRequest(request, recipientId))));
        return responses;
    }

    @Override
    public AlertResponse handleLowStockEvent(LowStockEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productName", normalizeOptionalText(event.getProductName()));
        metadata.put("warehouseName", normalizeOptionalText(event.getWarehouseName()));
        metadata.put("availableQuantity", normalizeDecimal(event.getAvailableQuantity()));
        metadata.put("reorderLevel", normalizeDecimal(event.getReorderLevel()));
        metadata.put("eventTime", event.getEventTime());

        Alert alert = Alert.builder()
                .recipientId(event.getRecipientId())
                .type(AlertType.LOW_STOCK)
                .severity(event.getSeverity())
                .title("Low stock detected")
                .message(buildLowStockMessage(event))
                .relatedProductId(event.getProductId())
                .relatedWarehouseId(event.getWarehouseId())
                .channel(resolveEffectiveChannel(AlertChannel.IN_APP, event.getSeverity()))
                .metadata(toMetadata(metadata))
                .createdAt(event.getEventTime())
                .build();

        return alertMapper.toResponse(saveAlert(alert));
    }

    @Override
    public AlertResponse handleOverstockEvent(OverstockEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productName", normalizeOptionalText(event.getProductName()));
        metadata.put("warehouseName", normalizeOptionalText(event.getWarehouseName()));
        metadata.put("quantity", normalizeDecimal(event.getQuantity()));
        metadata.put("maxStockLevel", normalizeDecimal(event.getMaxStockLevel()));
        metadata.put("eventTime", event.getEventTime());

        Alert alert = Alert.builder()
                .recipientId(event.getRecipientId())
                .type(AlertType.OVERSTOCK)
                .severity(event.getSeverity())
                .title("Overstock threshold exceeded")
                .message(buildOverstockMessage(event))
                .relatedProductId(event.getProductId())
                .relatedWarehouseId(event.getWarehouseId())
                .channel(resolveEffectiveChannel(AlertChannel.IN_APP, event.getSeverity()))
                .metadata(toMetadata(metadata))
                .createdAt(event.getEventTime())
                .build();

        return alertMapper.toResponse(saveAlert(alert));
    }

    @Override
    public AlertResponse handlePoPendingApprovalEvent(PoPendingApprovalEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("supplierId", event.getSupplierId());
        metadata.put("poNumber", normalizeOptionalText(event.getPoNumber()));
        metadata.put("totalAmount", normalizeDecimal(event.getTotalAmount()));
        metadata.put("eventTime", event.getEventTime());

        Alert alert = Alert.builder()
                .recipientId(event.getRecipientId())
                .type(AlertType.PO_PENDING)
                .severity(event.getSeverity())
                .title("Purchase order pending approval")
                .message(buildPoPendingMessage(event))
                .relatedWarehouseId(event.getWarehouseId())
                .relatedPurchaseOrderId(event.getPurchaseOrderId())
                .channel(resolveEffectiveChannel(AlertChannel.IN_APP, event.getSeverity()))
                .metadata(toMetadata(metadata))
                .createdAt(event.getEventTime())
                .build();

        return alertMapper.toResponse(saveAlert(alert));
    }

    @Override
    public AlertResponse handleOverdueReceiptEvent(OverdueReceiptEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("supplierId", event.getSupplierId());
        metadata.put("expectedDate", event.getExpectedDate());
        metadata.put("eventTime", event.getEventTime());

        Alert alert = Alert.builder()
                .recipientId(event.getRecipientId())
                .type(AlertType.OVERDUE_RECEIPT)
                .severity(event.getSeverity())
                .title("Overdue receipt requires action")
                .message(buildOverdueReceiptMessage(event))
                .relatedWarehouseId(event.getWarehouseId())
                .relatedPurchaseOrderId(event.getPurchaseOrderId())
                .channel(resolveEffectiveChannel(AlertChannel.IN_APP, event.getSeverity()))
                .metadata(toMetadata(metadata))
                .createdAt(event.getEventTime())
                .build();

        return alertMapper.toResponse(saveAlert(alert));
    }

    @Override
    public AlertResponse handleSystemAlertEvent(SystemAlertEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("eventTime", event.getEventTime());
        metadata.put("source", "SYSTEM_EVENT");

        Alert alert = Alert.builder()
                .recipientId(event.getRecipientId())
                .type(AlertType.SYSTEM)
                .severity(event.getSeverity())
                .title(normalizeRequiredText(event.getTitle(), "title is required."))
                .message(normalizeRequiredText(event.getMessage(), "message is required."))
                .channel(resolveEffectiveChannel(event.getChannel(), event.getSeverity()))
                .metadata(toMetadata(metadata))
                .createdAt(event.getEventTime())
                .build();

        return alertMapper.toResponse(saveAlert(alert));
    }

    @Override
    public AlertResponse markAsRead(Long alertId) {
        Alert alert = getAlertEntity(alertId);
        assertCanAccessAlert(alert);

        if (!Boolean.TRUE.equals(alert.getIsRead())) {
            alert.setIsRead(Boolean.TRUE);
            alert.setReadAt(LocalDateTime.now());
            alert = alertRepository.save(alert);
            LOGGER.info("Marked alert as read alertId={} recipientId={}", alert.getAlertId(), alert.getRecipientId());
        }

        return alertMapper.toResponse(alert);
    }

    @Override
    public void markAllRead(Long recipientId) {
        Long normalizedRecipientId = normalizeRequiredId(recipientId, "recipientId is required.");
        assertCanAccessRecipient(normalizedRecipientId);

        List<Alert> unreadAlerts = alertRepository.findByRecipientIdAndIsRead(normalizedRecipientId, Boolean.FALSE);
        if (unreadAlerts.isEmpty()) {
            return;
        }

        LocalDateTime readAt = LocalDateTime.now();
        unreadAlerts.forEach(alert -> {
            alert.setIsRead(Boolean.TRUE);
            alert.setReadAt(readAt);
        });
        alertRepository.saveAll(unreadAlerts);
        LOGGER.info("Marked all alerts as read recipientId={} count={}", normalizedRecipientId, unreadAlerts.size());
    }

    @Override
    public AlertResponse acknowledge(Long alertId) {
        Alert alert = getAlertEntity(alertId);
        assertCanAccessAlert(alert);

        if (!Boolean.TRUE.equals(alert.getIsAcknowledged())) {
            alert.setIsAcknowledged(Boolean.TRUE);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alert = alertRepository.save(alert);
            LOGGER.info("Acknowledged alert alertId={} recipientId={}", alert.getAlertId(), alert.getRecipientId());
        }

        return alertMapper.toResponse(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getByRecipient(Long recipientId) {
        Long normalizedRecipientId = normalizeRequiredId(recipientId, "recipientId is required.");
        assertCanAccessRecipient(normalizedRecipientId);
        return alertRepository.findByRecipientIdOrderByCreatedAtDesc(normalizedRecipientId).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertResponse> searchAlerts(AlertSearchRequest request) {
        AlertSearchRequest searchRequest = normalizeSearchRequest(request);
        Pageable pageable = buildPageable(
                searchRequest.getPage() == null ? 0 : searchRequest.getPage(),
                searchRequest.getSize() == null ? 20 : searchRequest.getSize(),
                searchRequest.getSortBy(),
                searchRequest.getSortDir());

        return alertRepository.findAll(AlertSpecifications.withFilters(searchRequest), pageable)
                .map(alertMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long recipientId) {
        Long normalizedRecipientId = normalizeRequiredId(recipientId, "recipientId is required.");
        assertCanAccessRecipient(normalizedRecipientId);
        return UnreadCountResponse.builder()
                .recipientId(normalizedRecipientId)
                .unreadCount(alertRepository.countByRecipientIdAndIsRead(normalizedRecipientId, Boolean.FALSE))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getUnacknowledged(Long recipientId) {
        Long normalizedRecipientId = normalizeRequiredId(recipientId, "recipientId is required.");
        assertCanAccessRecipient(normalizedRecipientId);
        return alertRepository.findByRecipientIdAndIsAcknowledgedOrderByCreatedAtDesc(normalizedRecipientId, Boolean.FALSE)
                .stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteAlert(Long alertId) {
        Alert alert = getAlertEntity(alertId);
        assertCanAccessAlert(alert);
        alertRepository.delete(alert);
        LOGGER.info("Deleted alert alertId={} recipientId={}", alert.getAlertId(), alert.getRecipientId());
    }

    private Alert saveAlert(Alert alert) {
        Alert savedAlert = alertRepository.save(alert);
        LOGGER.info("Saved alert alertId={} recipientId={} type={} severity={}",
                savedAlert.getAlertId(),
                savedAlert.getRecipientId(),
                savedAlert.getType(),
                savedAlert.getSeverity());
        dispatchEmailIfRequired(savedAlert);
        return savedAlert;
    }

    private void dispatchEmailIfRequired(Alert alert) {
        if (!shouldDispatchEmail(alert)) {
            return;
        }

        Optional<String> recipientEmail = resolveRecipientEmail(alert.getRecipientId());
        if (recipientEmail.isEmpty()) {
            LOGGER.warn("No recipient email available for alertId={} recipientId={}", alert.getAlertId(), alert.getRecipientId());
            return;
        }

        try {
            alertEventPublisher.publishEmailAlert(EmailAlertEvent.builder()
                    .recipientId(alert.getRecipientId())
                    .toEmail(recipientEmail.get())
                    .subject(buildEmailSubject(alert))
                    .body(buildEmailBody(alert))
                    .alertId(alert.getAlertId())
                    .severity(alert.getSeverity())
                    .build());
        } catch (Exception exception) {
            LOGGER.error("Failed to publish email alert event for alertId={}: {}", alert.getAlertId(), exception.getMessage(), exception);
        }
    }

    private Optional<String> resolveRecipientEmail(Long recipientId) {
        if (!emailLookupEnabled) {
            return Optional.empty();
        }

        try {
            return authServiceClient.getAllUsers().stream()
                    .filter(user -> recipientId.equals(user.getUserId()))
                    .map(AuthUserResponse::getEmail)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .findFirst();
        } catch (Exception exception) {
            LOGGER.warn("Unable to resolve recipient email for recipientId={}: {}", recipientId, exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean shouldDispatchEmail(Alert alert) {
        return alert.getSeverity() == AlertSeverity.CRITICAL || alert.getChannel() == AlertChannel.BOTH;
    }

    private AlertChannel resolveEffectiveChannel(AlertChannel requestedChannel, AlertSeverity severity) {
        if (severity == AlertSeverity.CRITICAL) {
            return AlertChannel.BOTH;
        }
        if (requestedChannel == null || requestedChannel == AlertChannel.IN_APP) {
            return AlertChannel.IN_APP;
        }
        return AlertChannel.BOTH;
    }

    private Alert getAlertEntity(Long alertId) {
        Long normalizedAlertId = normalizeRequiredId(alertId, "alertId is required.");
        return alertRepository.findById(normalizedAlertId)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + normalizedAlertId));
    }

    private void assertCanAccessAlert(Alert alert) {
        assertCanAccessRecipient(alert.getRecipientId());
    }

    private void assertCanAccessRecipient(Long recipientId) {
        if (securityUtils.hasAnyRole("ADMIN")) {
            return;
        }

        AuthenticatedUser currentUser = securityUtils.getCurrentUser();
        if (!recipientId.equals(currentUser.userId())) {
            throw new UnauthorizedAlertAccessException("You do not have permission to access this alert.");
        }
    }

    private AlertSearchRequest normalizeSearchRequest(AlertSearchRequest request) {
        AlertSearchRequest resolvedRequest = request == null ? AlertSearchRequest.builder().build() : request;

        if (resolvedRequest.getRecipientId() != null && resolvedRequest.getRecipientId() <= 0) {
            throw new InvalidAlertRequestException("recipientId must be greater than zero.");
        }
        if (resolvedRequest.getStartDate() != null
                && resolvedRequest.getEndDate() != null
                && resolvedRequest.getEndDate().isBefore(resolvedRequest.getStartDate())) {
            throw new InvalidAlertRequestException("endDate must be greater than or equal to startDate.");
        }

        if (!securityUtils.hasAnyRole("ADMIN")) {
            AuthenticatedUser currentUser = securityUtils.getCurrentUser();
            if (resolvedRequest.getRecipientId() != null && !currentUser.userId().equals(resolvedRequest.getRecipientId())) {
                throw new UnauthorizedAlertAccessException("You can only search your own alerts.");
            }
            resolvedRequest.setRecipientId(currentUser.userId());
        }

        return resolvedRequest;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0 || size <= 0) {
            throw new InvalidAlertRequestException("page must be zero or greater and size must be greater than zero.");
        }
        return PageRequest.of(page, size, buildSort(sortBy, sortDir));
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String resolvedSortBy = StringUtils.hasText(sortBy) ? sortBy.trim() : "createdAt";
        if (!SORT_FIELDS.contains(resolvedSortBy)) {
            throw new InvalidAlertRequestException("Unsupported sortBy value: " + resolvedSortBy);
        }

        return Sort.by(parseSortDirection(sortDir), resolvedSortBy);
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        if (!StringUtils.hasText(sortDir)) {
            return Sort.Direction.DESC;
        }

        try {
            return Sort.Direction.fromString(sortDir.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAlertRequestException("sortDir must be either asc or desc.");
        }
    }

    private SendAlertRequest toSingleRequest(SendBulkAlertRequest request, Long recipientId) {
        return SendAlertRequest.builder()
                .recipientId(recipientId)
                .type(request.getType())
                .severity(request.getSeverity())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedProductId(request.getRelatedProductId())
                .relatedWarehouseId(request.getRelatedWarehouseId())
                .relatedPurchaseOrderId(request.getRelatedPurchaseOrderId())
                .channel(request.getChannel())
                .build();
    }

    private AlertType requireType(AlertType type) {
        if (type == null) {
            throw new InvalidAlertRequestException("type is required.");
        }
        return type;
    }

    private AlertSeverity requireSeverity(AlertSeverity severity) {
        if (severity == null) {
            throw new InvalidAlertRequestException("severity is required.");
        }
        return severity;
    }

    private Long normalizeRequiredId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new InvalidAlertRequestException(message);
        }
        return value;
    }

    private Long normalizeOptionalId(Long value) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new InvalidAlertRequestException("Related identifiers must be greater than zero.");
        }
        return value;
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidAlertRequestException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private String toMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Failed to serialize alert metadata: {}", exception.getMessage());
            return null;
        }
    }

    private String buildLowStockMessage(LowStockEvent event) {
        return "%s available quantity %s dropped below reorder level %s in %s."
                .formatted(
                        fallbackName(event.getProductName(), "Product " + event.getProductId()),
                        normalizeDecimal(event.getAvailableQuantity()),
                        normalizeDecimal(event.getReorderLevel()),
                        fallbackName(event.getWarehouseName(), "warehouse " + event.getWarehouseId()));
    }

    private String buildOverstockMessage(OverstockEvent event) {
        return "%s quantity %s exceeded max stock level %s in %s."
                .formatted(
                        fallbackName(event.getProductName(), "Product " + event.getProductId()),
                        normalizeDecimal(event.getQuantity()),
                        normalizeDecimal(event.getMaxStockLevel()),
                        fallbackName(event.getWarehouseName(), "warehouse " + event.getWarehouseId()));
    }

    private String buildPoPendingMessage(PoPendingApprovalEvent event) {
        return "Purchase order %s worth %s is pending approval for warehouse %s."
                .formatted(
                        normalizeRequiredText(event.getPoNumber(), "poNumber is required."),
                        normalizeDecimal(event.getTotalAmount()),
                        event.getWarehouseId());
    }

    private String buildOverdueReceiptMessage(OverdueReceiptEvent event) {
        return "Purchase order %s expected on %s has not been received for warehouse %s."
                .formatted(
                        event.getPurchaseOrderId(),
                        event.getExpectedDate(),
                        event.getWarehouseId());
    }

    private String fallbackName(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String buildEmailSubject(Alert alert) {
        return "[StockPro %s] %s".formatted(alert.getSeverity().name(), alert.getTitle());
    }

    private String buildEmailBody(Alert alert) {
        return switch (alert.getType()) {
            case LOW_STOCK -> """
                    Low stock alert

                    %s

                    Severity: %s
                    Created At: %s
                    Product Id: %s
                    Warehouse Id: %s
                    """
                    .formatted(
                            alert.getMessage(),
                            alert.getSeverity().name(),
                            formatDateTime(alert.getCreatedAt()),
                            alert.getRelatedProductId(),
                            alert.getRelatedWarehouseId());
            case OVERSTOCK -> """
                    Overstock alert

                    %s

                    Severity: %s
                    Created At: %s
                    Product Id: %s
                    Warehouse Id: %s
                    """
                    .formatted(
                            alert.getMessage(),
                            alert.getSeverity().name(),
                            formatDateTime(alert.getCreatedAt()),
                            alert.getRelatedProductId(),
                            alert.getRelatedWarehouseId());
            case PO_PENDING -> """
                    Purchase order pending approval

                    %s

                    Severity: %s
                    Created At: %s
                    Purchase Order Id: %s
                    Warehouse Id: %s
                    """
                    .formatted(
                            alert.getMessage(),
                            alert.getSeverity().name(),
                            formatDateTime(alert.getCreatedAt()),
                            alert.getRelatedPurchaseOrderId(),
                            alert.getRelatedWarehouseId());
            case OVERDUE_RECEIPT -> """
                    Overdue receipt alert

                    %s

                    Severity: %s
                    Created At: %s
                    Purchase Order Id: %s
                    Warehouse Id: %s
                    """
                    .formatted(
                            alert.getMessage(),
                            alert.getSeverity().name(),
                            formatDateTime(alert.getCreatedAt()),
                            alert.getRelatedPurchaseOrderId(),
                            alert.getRelatedWarehouseId());
            case SYSTEM -> """
                    System alert

                    %s

                    Severity: %s
                    Created At: %s
                    """
                    .formatted(
                            alert.getMessage(),
                            alert.getSeverity().name(),
                            formatDateTime(alert.getCreatedAt()));
        };
    }

    private String formatDateTime(LocalDateTime createdAt) {
        return createdAt == null ? LocalDateTime.now().format(DATE_TIME_FORMATTER) : createdAt.format(DATE_TIME_FORMATTER);
    }
}
