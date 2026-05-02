package com.stockpro.purchaseservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseAnalyticsResponse(
        BigDecimal totalSpend,
        BigDecimal monthlySpend,
        List<String> topSuppliers,
        List<String> topPurchasedProducts,
        long pendingApprovals,
        long overdueReceipts,
        long averageApprovalTime,
        long averageDeliveryDelay) {
}
