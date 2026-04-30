package com.stockpro.alertservice.service;

import com.stockpro.alertservice.dto.AlertResponseDTO;
import com.stockpro.alertservice.entity.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponseDTO toResponse(Alert alert) {
        return AlertResponseDTO.builder()
                .alertId(alert.getAlertId())
                .recipientId(alert.getRecipientId())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .relatedProductId(alert.getRelatedProductId())
                .relatedWarehouseId(alert.getRelatedWarehouseId())
                .relatedPurchaseOrderId(alert.getRelatedPurchaseOrderId())
                .channel(alert.getChannel())
                .isRead(alert.getIsRead())
                .isAcknowledged(alert.getIsAcknowledged())
                .readAt(alert.getReadAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
