package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record LowStockReportItem(
        Long productId,
        String sku,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal availableQuantity,
        BigDecimal reorderLevel,
        BigDecimal shortageQuantity,
        String severity) {
}
