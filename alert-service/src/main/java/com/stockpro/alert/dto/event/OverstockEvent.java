package com.stockpro.alert.dto.event;

import com.stockpro.alert.enums.AlertSeverity;
import jakarta.validation.constraints.DecimalMin;
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
public class OverstockEvent {

    @NotNull(message = "recipientId is required.")
    @Positive(message = "recipientId must be greater than zero.")
    private Long recipientId;

    @NotNull(message = "productId is required.")
    @Positive(message = "productId must be greater than zero.")
    private Long productId;

    @NotNull(message = "warehouseId is required.")
    @Positive(message = "warehouseId must be greater than zero.")
    private Long warehouseId;

    private String productName;
    private String warehouseName;

    @NotNull(message = "quantity is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "quantity cannot be negative.")
    private BigDecimal quantity;

    @NotNull(message = "maxStockLevel is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "maxStockLevel cannot be negative.")
    private BigDecimal maxStockLevel;

    @NotNull(message = "severity is required.")
    private AlertSeverity severity;

    @NotNull(message = "eventTime is required.")
    private LocalDateTime eventTime;
}
