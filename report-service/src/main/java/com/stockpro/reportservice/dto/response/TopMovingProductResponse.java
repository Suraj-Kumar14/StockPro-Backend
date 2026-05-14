package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record TopMovingProductResponse(
        Long productId,
        String productName,
        String sku,
        BigDecimal unitsIn,
        BigDecimal unitsOut,
        BigDecimal totalMoved,
        long movementCount,
        BigDecimal totalMovementValue) {
}
