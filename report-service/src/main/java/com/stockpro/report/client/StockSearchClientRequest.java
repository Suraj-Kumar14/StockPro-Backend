package com.stockpro.report.client;

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
public class StockSearchClientRequest {

    private Long warehouseId;
    private Long productId;
    private String location;
    private Boolean lowStockOnly;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDir;
}
