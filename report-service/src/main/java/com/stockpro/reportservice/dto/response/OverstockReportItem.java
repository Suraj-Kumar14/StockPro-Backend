package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record OverstockReportItem(
        Long productId,
        String sku,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal maxStockLevel,
        BigDecimal excessQuantity) {
}
