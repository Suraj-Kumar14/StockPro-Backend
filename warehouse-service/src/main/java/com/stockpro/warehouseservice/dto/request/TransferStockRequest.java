package com.stockpro.warehouseservice.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferStockRequest {
    @NotNull private Long productId;
    @NotNull private Long sourceWarehouseId;
    @NotNull
    @JsonAlias("targetWarehouseId")
    private Long destinationWarehouseId;
    @NotNull
    @Min(1)
    private Integer quantity;
    @JsonAlias("reason")
    private String reasonCode;
    private String notes;
}
