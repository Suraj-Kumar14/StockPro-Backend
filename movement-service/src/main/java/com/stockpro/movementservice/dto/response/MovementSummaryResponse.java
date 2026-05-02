package com.stockpro.movementservice.dto.response;

import java.math.BigDecimal;

public record MovementSummaryResponse(
        long totalMovements,
        BigDecimal totalStockInQuantity,
        BigDecimal totalStockOutQuantity,
        BigDecimal totalTransferQuantity,
        BigDecimal totalAdjustmentQuantity,
        BigDecimal totalWriteOffQuantity,
        BigDecimal totalReturnQuantity,
        BigDecimal totalMovementValue,
        long movementsToday,
        long movementsThisMonth) {
}
