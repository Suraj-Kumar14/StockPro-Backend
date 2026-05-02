package com.stockpro.alertservice.dto.response;

import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertResponse {
    private Long alertId;
    private String alertNumber;
    private Long recipientId;
    private String recipientRole;
    private AlertType type;
    private AlertSeverity severity;
    private AlertStatus status;
    private AlertChannel channel;
    private String title;
    private String message;
    private Long relatedProductId;
    private Long relatedWarehouseId;
    private Long relatedPurchaseOrderId;
    private Long relatedSupplierId;
    private Long relatedMovementId;
    private String referenceType;
    private String referenceId;
    private String referenceNumber;
    private Boolean isRead;
    private Boolean isAcknowledged;
    private Boolean isDismissed;
    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime dismissedAt;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String sourceService;
    private String actionUrl;
    private String metadataJson;
}
