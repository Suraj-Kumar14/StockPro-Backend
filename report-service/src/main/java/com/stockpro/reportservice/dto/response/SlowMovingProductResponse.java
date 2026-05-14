package com.stockpro.reportservice.dto.response;

import java.time.LocalDate;
import java.math.BigDecimal;

public record SlowMovingProductResponse(
        Long productId,
        String productName,
        String sku,
        BigDecimal totalMoved,
        LocalDate lastMovementDate,
        long daysSinceLastMovement,
        BigDecimal currentQuantity) {
}
