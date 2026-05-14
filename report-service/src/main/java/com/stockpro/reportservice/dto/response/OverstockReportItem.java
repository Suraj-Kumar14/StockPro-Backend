package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record OverstockReportItem(
        Long productId,
        String productName,
        String sku,
        Long warehouseId,
        String warehouseName,
        BigDecimal availableQuantity,
        BigDecimal reorderLevel,
        BigDecimal maxStockLevel,
        String severity,
        String recommendedAction) {
}
