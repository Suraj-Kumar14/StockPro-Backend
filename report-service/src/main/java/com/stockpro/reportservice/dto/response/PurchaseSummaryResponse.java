package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record PurchaseSummaryResponse(
        long totalPurchaseOrders,
        long pendingApprovalCount,
        long approvedCount,
        long receivedCount,
        long cancelledCount,
        long overdueCount,
        BigDecimal totalPurchaseValue,
        BigDecimal receivedPurchaseValue,
        BigDecimal pendingPurchaseValue) {
}
