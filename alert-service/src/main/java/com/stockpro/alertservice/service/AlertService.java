package com.stockpro.alertservice.service;

import com.stockpro.alertservice.dto.AlertRequestDTO;
import com.stockpro.alertservice.dto.AlertResponseDTO;
import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import com.stockpro.alertservice.exception.AlertNotFoundException;
import com.stockpro.alertservice.exception.InvalidAlertException;
import com.stockpro.alertservice.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertValidationService validationService;
    private final AlertMapper alertMapper;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@stockpro.com}")
    private String fromEmail;

    @Value("${stockpro.alerts.default-recipient-id:1}")
    private Long defaultRecipientId;

    @Transactional
    public AlertResponseDTO createAlert(AlertRequestDTO dto) {
        AlertRequestDTO normalizedRequest = normalizeRequest(dto);
        validationService.validateRequest(normalizedRequest);

        Alert alert = mapToEntity(normalizedRequest);
        Alert savedAlert = saveAlert(alert);

        triggerCriticalEmail(savedAlert);
        log.info("Created alert {} type {} severity {} for recipient {}",
                savedAlert.getAlertId(), savedAlert.getType(),
                savedAlert.getSeverity(), savedAlert.getRecipientId());
        return alertMapper.toResponse(savedAlert);
    }

    @Transactional
    public List<AlertResponseDTO> sendBulkAlerts(List<AlertRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidAlertException("At least one alert request is required");
        }
        return requests.stream()
                .map(this::createAlert)
                .toList();
    }

    public AlertResponseDTO getAlertById(Long id) {
        return alertMapper.toResponse(getAlert(id));
    }

    public List<AlertResponseDTO> getAlertsByRecipient(Long recipientId) {
        return alertRepository.findByRecipientIdAndIsArchivedFalse(recipientId).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    public List<AlertResponseDTO> getUnreadAlerts(Long recipientId) {
        return alertRepository.findUnreadAlertsByRecipient(recipientId).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    public List<AlertResponseDTO> getUnacknowledgedCriticalAlerts(Long recipientId) {
        return alertRepository.findUnacknowledgedCriticalAlerts(recipientId).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    public Long getUnreadCount(Long recipientId) {
        return alertRepository.countByRecipientIdAndIsReadAndIsArchivedFalse(recipientId, false);
    }

    public List<AlertResponseDTO> getRecentAlerts(int days) {
        if (days < 1) {
            throw new InvalidAlertException("Days must be greater than zero");
        }
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return alertRepository.findRecentAlerts(startDate).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    public List<AlertResponseDTO> getByType(AlertType type) {
        return alertRepository.findByTypeAndIsArchivedFalse(type).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    public List<AlertResponseDTO> getBySeverity(Severity severity) {
        return alertRepository.findBySeverityAndIsArchivedFalse(severity).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    public List<AlertResponseDTO> getUnacknowledged() {
        return alertRepository.findUnacknowledgedAlerts().stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    @Transactional
    public void markAsRead(Long alertId) {
        Alert alert = getAlert(alertId);
        if (Boolean.TRUE.equals(alert.getIsRead())) {
            return;
        }

        alert.setIsRead(true);
        alert.setReadAt(LocalDateTime.now());
        saveAlert(alert);
        log.info("Marked alert {} as read", alertId);
    }

    @Transactional
    public void markAllAsRead(Long recipientId) {
        List<Alert> unreadAlerts = alertRepository.findByRecipientIdAndIsReadAndIsArchivedFalse(recipientId, false);
        LocalDateTime now = LocalDateTime.now();
        unreadAlerts.forEach(alert -> {
            alert.setIsRead(true);
            alert.setReadAt(now);
        });
        alertRepository.saveAll(unreadAlerts);
        log.info("Marked {} alerts as read for recipient {}", unreadAlerts.size(), recipientId);
    }

    @Transactional
    public void acknowledge(Long alertId) {
        Alert alert = getAlert(alertId);
        if (Boolean.TRUE.equals(alert.getIsAcknowledged())) {
            return;
        }
        if (Boolean.FALSE.equals(alert.getIsRead())) {
            alert.setIsRead(true);
            alert.setReadAt(LocalDateTime.now());
        }

        alert.setIsAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        saveAlert(alert);
        log.info("Acknowledged alert {}", alertId);
    }

    @Transactional
    public void deleteAlert(Long alertId) {
        Alert alert = getAlert(alertId);
        alert.setIsArchived(true);
        saveAlert(alert);
        log.info("Archived alert {}", alertId);
    }

    public AlertRequestDTO buildLowStockAlert(Long recipientId, Long productId, Long warehouseId,
            String productName, String warehouseName, Integer availableQuantity, Integer reorderLevel) {
        Severity severity = availableQuantity != null && reorderLevel != null
                && availableQuantity <= Math.max(0, reorderLevel / 2)
                ? Severity.CRITICAL : Severity.WARNING;

        AlertRequestDTO request = baseRequest(recipientId, AlertType.LOW_STOCK, severity,
                "Low Stock Alert",
                String.format("Product '%s' in '%s' is low. Current: %d, reorder level: %d.",
                        productName, warehouseName, availableQuantity, reorderLevel));
        request.setRelatedProductId(productId);
        request.setRelatedWarehouseId(warehouseId);
        request.setChannel("IN_APP");
        return request;
    }

    public AlertRequestDTO buildOverstockAlert(Long recipientId, Long productId, Long warehouseId,
            String productName, String warehouseName, Integer quantity, Integer maxStockLevel) {
        Severity severity = quantity != null && maxStockLevel != null && quantity >= maxStockLevel * 2
                ? Severity.WARNING : Severity.INFO;

        AlertRequestDTO request = baseRequest(recipientId, AlertType.OVERSTOCK, severity,
                "Overstock Alert",
                String.format("Product '%s' in '%s' exceeded max stock. Current: %d, max: %d.",
                        productName, warehouseName, quantity, maxStockLevel));
        request.setRelatedProductId(productId);
        request.setRelatedWarehouseId(warehouseId);
        request.setChannel("IN_APP");
        return request;
    }

    public AlertRequestDTO buildPendingPoAlert(Long recipientId, Long purchaseOrderId, String message) {
        AlertRequestDTO request = baseRequest(resolveRecipient(recipientId), AlertType.PO_PENDING,
                Severity.WARNING, "PO Pending Approval", message);
        request.setRelatedPurchaseOrderId(purchaseOrderId);
        request.setChannel("IN_APP");
        return request;
    }

    public AlertRequestDTO buildOverdueReceiptAlert(Long recipientId, Long purchaseOrderId, String message) {
        AlertRequestDTO request = baseRequest(resolveRecipient(recipientId), AlertType.OVERDUE_RECEIPT,
                Severity.CRITICAL, "Overdue Purchase Order", message);
        request.setRelatedPurchaseOrderId(purchaseOrderId);
        request.setChannel("IN_APP");
        return request;
    }

    private Alert getAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with ID: " + id));
        if (Boolean.TRUE.equals(alert.getIsArchived())) {
            throw new AlertNotFoundException("Alert not found with ID: " + id);
        }
        return alert;
    }

    private AlertRequestDTO normalizeRequest(AlertRequestDTO dto) {
        AlertRequestDTO normalized = new AlertRequestDTO();
        normalized.setRecipientId(resolveRecipient(dto.getRecipientId()));
        normalized.setType(normalizeType(dto.getType()));
        normalized.setSeverity(dto.getSeverity());
        normalized.setTitle(trimToNull(dto.getTitle()));
        normalized.setMessage(trimToNull(dto.getMessage()));
        normalized.setRelatedProductId(dto.getRelatedProductId());
        normalized.setRelatedWarehouseId(dto.getRelatedWarehouseId());
        normalized.setRelatedPurchaseOrderId(dto.getRelatedPurchaseOrderId());
        normalized.setChannel(normalizeChannel(dto.getChannel(), dto.getSeverity()));
        return normalized;
    }

    private Alert mapToEntity(AlertRequestDTO dto) {
        return Alert.builder()
                .recipientId(dto.getRecipientId())
                .type(dto.getType())
                .severity(dto.getSeverity())
                .title(dto.getTitle())
                .message(dto.getMessage())
                .relatedProductId(dto.getRelatedProductId())
                .relatedWarehouseId(dto.getRelatedWarehouseId())
                .relatedPurchaseOrderId(dto.getRelatedPurchaseOrderId())
                .channel(dto.getChannel())
                .isRead(false)
                .isAcknowledged(false)
                .isArchived(false)
                .build();
    }

    private Alert saveAlert(Alert alert) {
        try {
            return alertRepository.save(alert);
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidAlertException("Alert data violates a persistence constraint");
        }
    }

    private void triggerCriticalEmail(Alert alert) {
        if (alert.getSeverity() == Severity.CRITICAL) {
            sendEmail(alert);
        }
    }

    @Async
    public void sendEmail(Alert alert) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo("recipient@example.com");
            message.setSubject("[CRITICAL] " + alert.getTitle());
            message.setText(alert.getMessage());
            mailSender.send(message);
            log.info("Sent email for critical alert {}", alert.getAlertId());
        } catch (Exception ex) {
            log.error("Failed to send email for alert {}", alert.getAlertId(), ex);
        }
    }

    private AlertRequestDTO baseRequest(Long recipientId, AlertType type, Severity severity,
            String title, String message) {
        AlertRequestDTO request = new AlertRequestDTO();
        request.setRecipientId(recipientId);
        request.setType(type);
        request.setSeverity(severity);
        request.setTitle(title);
        request.setMessage(message);
        return request;
    }

    private Long resolveRecipient(Long recipientId) {
        return recipientId == null ? defaultRecipientId : recipientId;
    }

    private AlertType normalizeType(AlertType type) {
        if (type == AlertType.PO_PENDING_APPROVAL) {
            return AlertType.PO_PENDING;
        }
        if (type == AlertType.SYSTEM_NOTIFICATION) {
            return AlertType.SYSTEM;
        }
        return type;
    }

    private String normalizeChannel(String channel, Severity severity) {
        String normalized = trimToNull(channel);
        if (normalized == null) {
            return severity == Severity.CRITICAL ? "EMAIL" : "IN_APP";
        }
        return normalized.toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
