package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExecutiveDashboardResponse(
        long totalProducts,
        long activeProducts,
        long totalWarehouses,
        BigDecimal inventoryValue,
        long lowStockItems,
        long overstockItems,
        long pendingPoApprovals,
        long overduePurchaseOrders,
        BigDecimal totalPurchaseValue,
        BigDecimal totalPaidAmount,
        long payablePurchaseOrders,
        long cancelledPurchaseOrders,
        Long activeUsers,
        long criticalAlerts,
        long stockMovementToday,
        List<TopMovingProductResponse> topMovingProducts,
        List<DashboardAlertItem> recentAlerts,
        List<TrendPointResponse> valuationTrend,
        List<TrendPointResponse> purchaseTrend,
        List<String> warnings,
        List<String> unavailableSections) {
}
