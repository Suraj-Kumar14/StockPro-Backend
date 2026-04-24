package com.stockpro.warehouse.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
@Schema(description = "Request used to release reserved stock.")
public class ReleaseReservationRequest {

    @NotNull(message = "warehouseId is required.")
    @Positive(message = "warehouseId must be greater than zero.")
    @Schema(example = "1")
    private Long warehouseId;

    @NotNull(message = "productId is required.")
    @Positive(message = "productId must be greater than zero.")
    @Schema(example = "501")
    private Long productId;

    @NotNull(message = "quantity is required.")
    @DecimalMin(value = "0.0001", inclusive = true, message = "quantity must be greater than zero.")
    @Schema(example = "2.0000")
    private BigDecimal quantity;

    @Positive(message = "referenceId must be greater than zero.")
    @Schema(example = "12001")
    private Long referenceId;

    @Size(max = 100, message = "referenceType cannot exceed 100 characters.")
    @Schema(example = "SALES_ORDER")
    private String referenceType;

    @Size(max = 1000, message = "notes cannot exceed 1000 characters.")
    @Schema(example = "Released because SO-12001 line was cancelled")
    private String notes;
}
