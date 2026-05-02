package com.stockpro.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummaryResponse {

    private long totalProducts;
    private long activeProducts;
    private long inactiveProducts;
    private long categoriesCount;
    private long brandsCount;
}
