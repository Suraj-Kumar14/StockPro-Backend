package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record TopMovingProductResponse(
        Long productId,
        String sku,
        String productName,
        BigDecimal totalMovementQuantity,
        long movementCount,
        BigDecimal totalMovementValue) {
}
