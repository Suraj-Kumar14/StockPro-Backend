package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record StockSummaryResponse(
        long totalProducts,
        long totalWarehouses,
        BigDecimal totalStockQuantity,
        BigDecimal totalReservedQuantity,
        BigDecimal totalAvailableQuantity,
        long lowStockCount,
        long overstockCount,
        long outOfStockCount) {
}
