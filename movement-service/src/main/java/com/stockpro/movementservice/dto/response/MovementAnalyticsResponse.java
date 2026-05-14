package com.stockpro.movementservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MovementAnalyticsResponse(
        Map<String, Long> movementCountByType,
        Map<String, Long> movementCountByWarehouse,
        Map<String, Long> movementCountByProduct,
        Map<String, BigDecimal> dailyMovementTrend,
        List<MovementVolumeItem> topMovedProducts,
        List<MovementResponse> highestValueMovements,
        List<TrendPoint> adjustmentTrend,
        List<TrendPoint> writeOffTrend) {

    public record MovementVolumeItem(
            Long productId,
            String productName,
            BigDecimal quantity,
            BigDecimal totalValue) {
    }

    public record TrendPoint(
            String date,
            BigDecimal quantity) {
    }
}
