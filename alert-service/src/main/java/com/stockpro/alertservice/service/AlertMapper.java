package com.stockpro.alertservice.service;

import com.stockpro.alertservice.dto.response.AlertResponse;
import com.stockpro.alertservice.entity.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
                .alertId(alert.getAlertId())
                .alertNumber(alert.getAlertNumber())
                .recipientId(alert.getRecipientId())
                .recipientRole(alert.getRecipientRole())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .channel(alert.getChannel())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .relatedProductId(alert.getRelatedProductId())
                .relatedWarehouseId(alert.getRelatedWarehouseId())
                .relatedPurchaseOrderId(alert.getRelatedPurchaseOrderId())
                .relatedSupplierId(alert.getRelatedSupplierId())
                .relatedMovementId(alert.getRelatedMovementId())
                .referenceType(alert.getReferenceType())
                .referenceId(alert.getReferenceId())
                .referenceNumber(alert.getReferenceNumber())
                .isRead(alert.getIsRead())
                .isAcknowledged(alert.getIsAcknowledged())
                .isDismissed(alert.getIsDismissed())
                .readAt(alert.getReadAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .dismissedAt(alert.getDismissedAt())
                .createdAt(alert.getCreatedAt())
                .expiresAt(alert.getExpiresAt())
                .sourceService(alert.getSourceService())
                .actionUrl(alert.getActionUrl())
                .metadataJson(alert.getMetadataJson())
                .build();
    }
}
