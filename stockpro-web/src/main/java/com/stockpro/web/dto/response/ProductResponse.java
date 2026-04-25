package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private BigDecimal reorderLevel;
    private BigDecimal maxStockLevel;
    private Integer leadTimeDays;
    private String imageUrl;
    private String barcode;

    @JsonAlias({ "active", "isActive" })
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
