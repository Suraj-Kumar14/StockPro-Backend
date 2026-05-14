package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CancelledPurchaseOrderReportResponse(
        long cancelledCount,
        BigDecimal cancelledValue,
        List<PurchaseSpendBreakdownItem> supplierBreakdown,
        List<PurchaseSpendBreakdownItem> warehouseBreakdown,
        List<CancelledPurchaseOrderRowResponse> cancelledOrders) {
}
