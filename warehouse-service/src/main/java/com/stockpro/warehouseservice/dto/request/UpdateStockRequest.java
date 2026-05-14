package com.stockpro.warehouseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStockRequest {
    @NotNull private Long warehouseId;
    @NotNull private Long productId;
    @NotNull
    @Min(0) private Integer quantity;
    private String reason;
    private String notes;
}
