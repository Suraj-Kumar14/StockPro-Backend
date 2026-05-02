package com.stockpro.purchaseservice.dto.response;

import java.math.BigDecimal;

public record PurchaseOrderSummaryResponse(
        long totalPurchaseOrders,
        long draftCount,
        long pendingApprovalCount,
        long approvedCount,
        long partiallyReceivedCount,
        long receivedCount,
        long cancelledCount,
        long rejectedCount,
        long overdueCount,
        BigDecimal totalPurchaseValue,
        BigDecimal pendingPurchaseValue,
        BigDecimal receivedPurchaseValue) {
}
