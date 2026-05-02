package com.stockpro.warehouseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferStockRequest {
    @NotNull private Long productId;
    @NotNull private Long sourceWarehouseId;
    @NotNull private Long destinationWarehouseId;
    @Min(1) private Integer quantity;
    private String reasonCode;
    private String notes;
}
