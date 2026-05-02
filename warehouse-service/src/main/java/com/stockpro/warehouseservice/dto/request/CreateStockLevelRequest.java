package com.stockpro.warehouseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateStockLevelRequest {
    @NotNull private Long warehouseId;
    @NotNull private Long productId;
    @Min(0) private Integer quantity;
    @Min(0) private Integer reservedQuantity;
    private String locationCode;
}
