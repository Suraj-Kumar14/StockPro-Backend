package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LowStockProductResponse {

    private Long productId;
    private String sku;
    private String name;
    private BigDecimal reorderLevel;
    private BigDecimal maxStockLevel;
    private String barcode;
    private String category;
    private String brand;
}
