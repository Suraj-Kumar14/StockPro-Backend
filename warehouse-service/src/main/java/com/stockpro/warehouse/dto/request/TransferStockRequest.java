package com.stockpro.warehouse.dto.request;

import com.stockpro.warehouse.validation.ValidWarehouseTransfer;
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
@ValidWarehouseTransfer
@Schema(description = "Request used to transfer stock atomically between two warehouses.")
public class TransferStockRequest {

    @NotNull(message = "sourceWarehouseId is required.")
    @Positive(message = "sourceWarehouseId must be greater than zero.")
    @Schema(example = "1")
    private Long sourceWarehouseId;

    @NotNull(message = "destinationWarehouseId is required.")
    @Positive(message = "destinationWarehouseId must be greater than zero.")
    @Schema(example = "2")
    private Long destinationWarehouseId;

    @NotNull(message = "productId is required.")
    @Positive(message = "productId must be greater than zero.")
    @Schema(example = "501")
    private Long productId;

    @NotNull(message = "quantity is required.")
    @DecimalMin(value = "0.0001", inclusive = true, message = "quantity must be greater than zero.")
    @Schema(example = "12.0000")
    private BigDecimal quantity;

    @Positive(message = "referenceId must be greater than zero.")
    @Schema(example = "15001")
    private Long referenceId;

    @Size(max = 100, message = "referenceType cannot exceed 100 characters.")
    @Schema(example = "WAREHOUSE_TRANSFER")
    private String referenceType;

    @DecimalMin(value = "0.0", inclusive = true, message = "unitCost cannot be negative.")
    @Schema(example = "149.5000")
    private BigDecimal unitCost;

    @Size(max = 1000, message = "notes cannot exceed 1000 characters.")
    @Schema(example = "Redistribution to south fulfillment center")
    private String notes;
}
