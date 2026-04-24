package com.stockpro.movement.dto.request;

import com.stockpro.movement.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request body used to append a new immutable stock movement.")
public class RecordMovementRequest {

    @NotNull(message = "productId is required.")
    @Positive(message = "productId must be greater than zero.")
    private Long productId;

    @NotNull(message = "warehouseId is required.")
    @Positive(message = "warehouseId must be greater than zero.")
    private Long warehouseId;

    @NotNull(message = "movementType is required.")
    private MovementType movementType;

    @NotNull(message = "quantity is required.")
    @DecimalMin(value = "0.0001", inclusive = true, message = "quantity must be greater than zero.")
    private BigDecimal quantity;

    @Positive(message = "referenceId must be greater than zero.")
    private Long referenceId;

    @Size(max = 100, message = "referenceType cannot be longer than 100 characters.")
    private String referenceType;

    @DecimalMin(value = "0.0", inclusive = true, message = "unitCost cannot be negative.")
    private BigDecimal unitCost;

    @Positive(message = "performedBy must be greater than zero.")
    private Long performedBy;

    @Size(max = 1000, message = "notes cannot be longer than 1000 characters.")
    private String notes;

    @NotNull(message = "movementDate is required.")
    private LocalDateTime movementDate;

    @NotNull(message = "balanceAfter is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "balanceAfter cannot be negative.")
    private BigDecimal balanceAfter;
}
