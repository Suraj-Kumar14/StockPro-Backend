package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RejectedPurchaseOrderReportResponse(
        long rejectedCount,
        BigDecimal rejectedValue,
        List<PurchaseSpendBreakdownItem> supplierBreakdown,
        List<PurchaseSpendBreakdownItem> warehouseBreakdown,
        List<CancelledPurchaseOrderRowResponse> rejectedOrders) {
}
