package com.stockpro.web.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {

    private String name;
    private String category;
    private String brand;
    private String sku;
    private String barcode;
    private Boolean active;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    private Integer size = 20;

    private String sortBy = "productId";
    private String sortDir = "asc";
}
