package com.stockpro.warehouse.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request sent to movement-service after warehouse stock changes are committed in this service.")
public class RecordMovementRequest {

    @NotNull
    @Positive
    private Long productId;

    @NotNull
    @Positive
    private Long warehouseId;

    @NotBlank
    private String movementType;

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal quantity;

    @Positive
    private Long referenceId;

    private String referenceType;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal unitCost;

    @NotNull
    @Positive
    private Long performedBy;

    private String notes;

    @NotNull
    private LocalDateTime movementDate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal balanceAfter;
}
