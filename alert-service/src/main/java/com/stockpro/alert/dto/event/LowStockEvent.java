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
public class LowStockEvent {

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

    @NotNull(message = "availableQuantity is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "availableQuantity cannot be negative.")
    private BigDecimal availableQuantity;

    @NotNull(message = "reorderLevel is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "reorderLevel cannot be negative.")
    private BigDecimal reorderLevel;

    @NotNull(message = "severity is required.")
    private AlertSeverity severity;

    @NotNull(message = "eventTime is required.")
    private LocalDateTime eventTime;
}
