package com.stockpro.warehouseservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductLookupResponseDTO {

    private Long productId;
    private String sku;
    private String name;
    private String category;
    private String brand;
    private String barcode;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private Boolean isActive;
}
