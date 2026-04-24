package com.stockpro.warehouse.dto.request;

import com.stockpro.warehouse.validation.NonZero;
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
@Schema(description = "Stock adjustment request. Positive quantity increases stock, negative quantity decreases stock.")
public class UpdateStockRequest {

    @NotNull(message = "warehouseId is required.")
    @Positive(message = "warehouseId must be greater than zero.")
    @Schema(example = "1")
    private Long warehouseId;

    @NotNull(message = "productId is required.")
    @Positive(message = "productId must be greater than zero.")
    @Schema(example = "501")
    private Long productId;

    @NotNull(message = "quantity is required.")
    @NonZero(message = "quantity must be non-zero.")
    @Schema(example = "25.0000", description = "Positive for stock in, negative for stock out or adjustment.")
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "unitCost cannot be negative.")
    @Schema(example = "149.5000")
    private BigDecimal unitCost;

    @Positive(message = "referenceId must be greater than zero.")
    @Schema(example = "9001")
    private Long referenceId;

    @Size(max = 100, message = "referenceType cannot exceed 100 characters.")
    @Schema(example = "PURCHASE_ORDER")
    private String referenceType;

    @Size(max = 255, message = "location cannot exceed 255 characters.")
    @Schema(example = "A-01-RACK-03")
    private String location;

    @Size(max = 1000, message = "notes cannot exceed 1000 characters.")
    @Schema(example = "Goods received against PO-9001")
    private String notes;
}
