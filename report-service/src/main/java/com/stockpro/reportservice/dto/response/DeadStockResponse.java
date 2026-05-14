package com.stockpro.reportservice.dto.response;

import java.time.LocalDate;
import java.math.BigDecimal;

public record DeadStockResponse(
        Long productId,
        String productName,
        String sku,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal stockValue,
        LocalDate lastMovementDate,
        long daysWithoutMovement) {
}
