package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record ProductMovementSummaryResponse(
        Long productId,
        String productName,
        String sku,
        Long warehouseId,
        String warehouseName,
        BigDecimal unitsIn,
        BigDecimal unitsOut,
        BigDecimal totalMoved,
        long movementCount,
        BigDecimal movementValue) {
}
