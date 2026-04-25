package com.stockpro.alert.mapper;

import com.stockpro.alert.dto.response.AlertResponse;
import com.stockpro.alert.dto.response.AlertSummaryResponse;
import com.stockpro.alert.entity.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
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
                .createdAt(alert.getCreatedAt())
                .readAt(alert.getReadAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .build();
    }

    public AlertSummaryResponse toSummaryResponse(Alert alert) {
        return AlertSummaryResponse.builder()
                .alertId(alert.getAlertId())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .isRead(alert.getIsRead())
                .isAcknowledged(alert.getIsAcknowledged())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
