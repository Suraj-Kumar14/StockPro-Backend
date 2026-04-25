package com.stockpro.web.dto.request;

import com.stockpro.web.dto.AlertChannel;
import com.stockpro.web.dto.AlertSeverity;
import com.stockpro.web.dto.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAlertRequest {

    @NotNull(message = "Recipient is required.")
    @Positive(message = "Recipient is invalid.")
    private Long recipientId;

    @NotNull(message = "Alert type is required.")
    private AlertType type = AlertType.SYSTEM;

    @NotNull(message = "Severity is required.")
    private AlertSeverity severity = AlertSeverity.INFO;

    @NotBlank(message = "Title is required.")
    private String title;

    @NotBlank(message = "Message is required.")
    private String message;

    private Long relatedProductId;
    private Long relatedWarehouseId;
    private Long relatedPurchaseOrderId;

    @NotNull(message = "Channel is required.")
    private AlertChannel channel = AlertChannel.IN_APP;
}
