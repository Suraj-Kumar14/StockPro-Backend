package com.stockpro.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response used for low stock listing.
 * It combines product master data with live quantity received from warehouse-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowStockProductResponse {

    private Long productId;
    private String sku;
    private String name;
    private String category;
    private String brand;
    private String unitOfMeasure;
    private Integer reorderLevel;
    private Integer currentQuantity;
    private Boolean isActive;
    private String barcode;
}
