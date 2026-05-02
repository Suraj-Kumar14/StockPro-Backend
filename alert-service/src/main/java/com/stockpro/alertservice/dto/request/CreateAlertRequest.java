package com.stockpro.alertservice.dto.request;

import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CreateAlertRequest {
    private Long recipientId;
    private String recipientRole;
    @NotNull
    private AlertType type;
    @NotNull
    private AlertSeverity severity;
    @NotNull
    private AlertChannel channel;
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    private Long relatedProductId;
    private Long relatedWarehouseId;
    private Long relatedPurchaseOrderId;
    private Long relatedSupplierId;
    private Long relatedMovementId;
    private String referenceType;
    private String referenceId;
    private String referenceNumber;
    private LocalDateTime expiresAt;
    private String actionUrl;
    private String metadataJson;
}
