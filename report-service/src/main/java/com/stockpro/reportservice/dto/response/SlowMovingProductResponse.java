package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SlowMovingProductResponse(
        Long productId,
        String sku,
        String productName,
        LocalDateTime lastMovementDate,
        long daysSinceLastMovement,
        BigDecimal currentQuantity,
        BigDecimal stockValue) {
}
