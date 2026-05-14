package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record InventoryTurnoverProductItem(
        Long productId,
        String productName,
        String sku,
        BigDecimal cogs,
        BigDecimal averageInventoryValue,
        BigDecimal turnoverRate) {
}
