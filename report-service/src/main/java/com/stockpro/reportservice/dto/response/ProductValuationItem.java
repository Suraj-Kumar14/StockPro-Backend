package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record ProductValuationItem(
        Long productId,
        String sku,
        String productName,
        String category,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalValue) {
}
