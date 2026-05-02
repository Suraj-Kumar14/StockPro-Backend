package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExecutiveDashboardResponse(
        long totalProducts,
        long totalWarehouses,
        BigDecimal totalInventoryValue,
        long lowStockCount,
        long overstockCount,
        long pendingPurchaseApprovals,
        long overduePurchaseOrders,
        BigDecimal totalPurchaseValue,
        BigDecimal totalPaidAmount,
        long criticalAlerts,
        long stockMovementToday,
        List<TopMovingProductResponse> topMovingProducts,
        List<DashboardAlertItem> recentAlerts,
        List<TrendPointResponse> valuationTrend,
        List<TrendPointResponse> purchaseTrend,
        List<String> unavailableSections) {
}
