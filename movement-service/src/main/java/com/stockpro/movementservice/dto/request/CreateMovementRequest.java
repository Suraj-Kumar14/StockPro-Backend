package com.stockpro.movementservice.dto.request;

import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateMovementRequest(
        @NotNull(message = "Product is required") Long productId,
        @NotNull(message = "Warehouse is required") Long warehouseId,
        @NotNull(message = "Movement type is required") MovementType movementType,
        @NotNull(message = "Movement direction is required") MovementDirection direction,
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", inclusive = true, message = "Quantity must be positive") BigDecimal quantity,
        @DecimalMin(value = "0.0", inclusive = true, message = "Unit cost cannot be negative") BigDecimal unitCost,
        @NotNull(message = "Balance after is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Balance after cannot be negative") BigDecimal balanceAfter,
        ReferenceType referenceType,
        String referenceId,
        String referenceNumber,
        MovementReasonCode reasonCode,
        String notes,
        LocalDateTime movementDate,
        String sourceService,
        String correlationId) {
}
