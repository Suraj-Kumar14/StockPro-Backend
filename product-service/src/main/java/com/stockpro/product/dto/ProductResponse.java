package com.stockpro.product.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard product response returned by read and write APIs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long productId;
    private String sku;
    private String name;
    private String description;
    private String category;
    private String brand;
    private String unitOfMeasure;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private Integer leadTimeDays;
    private String imageUrl;
    private Boolean isActive;
    private String barcode;
}
