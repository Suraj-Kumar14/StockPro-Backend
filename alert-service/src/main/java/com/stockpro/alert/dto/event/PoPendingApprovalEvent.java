package com.stockpro.alert.dto.event;

import com.stockpro.alert.enums.AlertSeverity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class PoPendingApprovalEvent {

    @NotNull(message = "recipientId is required.")
    @Positive(message = "recipientId must be greater than zero.")
    private Long recipientId;

    @NotNull(message = "purchaseOrderId is required.")
    @Positive(message = "purchaseOrderId must be greater than zero.")
    private Long purchaseOrderId;

    @NotNull(message = "supplierId is required.")
    @Positive(message = "supplierId must be greater than zero.")
    private Long supplierId;

    @NotNull(message = "warehouseId is required.")
    @Positive(message = "warehouseId must be greater than zero.")
    private Long warehouseId;

    @NotBlank(message = "poNumber is required.")
    private String poNumber;

    @NotNull(message = "totalAmount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "totalAmount cannot be negative.")
    private BigDecimal totalAmount;

    @NotNull(message = "severity is required.")
    private AlertSeverity severity;

    @NotNull(message = "eventTime is required.")
    private LocalDateTime eventTime;
}
