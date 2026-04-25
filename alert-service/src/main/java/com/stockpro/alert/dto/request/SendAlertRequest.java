package com.stockpro.alert.dto.request;

import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body used to send a single alert.")
public class SendAlertRequest {

    @NotNull(message = "recipientId is required.")
    @Positive(message = "recipientId must be greater than zero.")
    @Schema(example = "101")
    private Long recipientId;

    @NotNull(message = "type is required.")
    @Schema(example = "SYSTEM")
    private AlertType type;

    @NotNull(message = "severity is required.")
    @Schema(example = "INFO")
    private AlertSeverity severity;

    @NotBlank(message = "title is required.")
    @Size(max = 255, message = "title cannot be longer than 255 characters.")
    @Schema(example = "StockPro maintenance reminder")
    private String title;

    @NotBlank(message = "message is required.")
    @Size(max = 1000, message = "message cannot be longer than 1000 characters.")
    @Schema(example = "The inventory dashboard will be under maintenance at 10:00 PM.")
    private String message;

    @Positive(message = "relatedProductId must be greater than zero.")
    @Schema(example = "501")
    private Long relatedProductId;

    @Positive(message = "relatedWarehouseId must be greater than zero.")
    @Schema(example = "1")
    private Long relatedWarehouseId;

    @Positive(message = "relatedPurchaseOrderId must be greater than zero.")
    @Schema(example = "9001")
    private Long relatedPurchaseOrderId;

    @NotNull(message = "channel is required.")
    @Schema(example = "IN_APP")
    private AlertChannel channel;
}
