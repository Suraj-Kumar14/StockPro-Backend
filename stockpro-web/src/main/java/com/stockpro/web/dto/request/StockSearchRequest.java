package com.stockpro.web.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchRequest {

    private Long warehouseId;
    private Long productId;
    private String location;
    private Boolean lowStockOnly;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    private Integer size = 20;

    private String sortBy = "lastUpdated";
    private String sortDir = "desc";
}
