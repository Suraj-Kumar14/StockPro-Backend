package com.stockpro.report.client;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductClientResponse {

    private Long productId;
    private String sku;
    private String name;
    private BigDecimal costPrice;
    private BigDecimal reorderLevel;
    private BigDecimal maxStockLevel;
    private Boolean isActive;
}
