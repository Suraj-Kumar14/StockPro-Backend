package com.stockpro.purchaseservice.dto;

import lombok.Data;

@Data
public class StockProductThresholdDTO {

    private Long productId;
    private Integer reorderLevel;
    private Integer maxStockLevel;
}
