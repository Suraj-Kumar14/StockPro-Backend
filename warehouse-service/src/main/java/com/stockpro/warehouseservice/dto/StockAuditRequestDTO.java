package com.stockpro.warehouseservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAuditRequestDTO {

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Counted quantity is required")
    @Min(value = 0, message = "Counted quantity cannot be negative")
    private Integer countedQuantity;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String binLocation;

    private Integer reorderLevel;
    private Integer maxStockLevel;
}
