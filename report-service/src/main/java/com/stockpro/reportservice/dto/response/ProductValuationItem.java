package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record ProductValuationItem(
        Long productId,
        String productName,
        String sku,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal costPrice,
        BigDecimal stockValue,
        String category) {
}
