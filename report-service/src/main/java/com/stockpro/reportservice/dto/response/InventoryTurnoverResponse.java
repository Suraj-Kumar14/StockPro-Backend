package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record InventoryTurnoverResponse(
        Long productId,
        String sku,
        String productName,
        BigDecimal openingStock,
        BigDecimal closingStock,
        BigDecimal averageInventory,
        BigDecimal stockOutQuantity,
        BigDecimal turnoverRatio) {
}
