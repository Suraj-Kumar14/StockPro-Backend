package com.stockpro.warehouseservice.dto.response;

import lombok.Builder;

@Builder
public record StockSummaryResponse(
        long totalStockItems,
        long totalQuantity,
        long totalReservedQuantity,
        long totalAvailableQuantity,
        long lowStockItemsCount,
        long overstockItemsCount) {
}
