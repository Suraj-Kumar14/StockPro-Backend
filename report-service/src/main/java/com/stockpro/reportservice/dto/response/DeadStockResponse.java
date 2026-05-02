package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeadStockResponse(
        Long productId,
        String sku,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal currentQuantity,
        BigDecimal stockValue,
        LocalDateTime lastMovementDate,
        long daysWithoutMovement) {
}
