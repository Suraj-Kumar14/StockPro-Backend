package com.stockpro.purchaseservice.dto;

import lombok.Data;

@Data
public class StockProductThresholdDTO {

    private Long productId;
    private String sku;
    private String name;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private Boolean isActive;
}
