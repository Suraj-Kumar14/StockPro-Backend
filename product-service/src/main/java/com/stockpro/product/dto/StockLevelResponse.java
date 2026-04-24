package com.stockpro.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response expected from warehouse-service for live stock levels.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevelResponse {

    private Long productId;
    private Integer currentQuantity;
}
