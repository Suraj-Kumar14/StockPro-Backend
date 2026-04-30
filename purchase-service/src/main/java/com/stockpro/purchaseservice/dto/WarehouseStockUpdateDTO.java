package com.stockpro.purchaseservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WarehouseStockUpdateDTO {

    private Long productId;
    private Integer quantity;
    private String binLocation;
    private Integer reorderLevel;
    private Integer maxStockLevel;
}
