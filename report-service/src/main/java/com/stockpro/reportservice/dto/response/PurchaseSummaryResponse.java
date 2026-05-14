package com.stockpro.reportservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PurchaseSummaryResponse(
        LocalDate from,
        LocalDate to,
        long totalPurchaseOrders,
        BigDecimal totalSpend,
        Map<String, Long> statusBreakdown,
        List<PurchaseSpendBreakdownItem> supplierBreakdown,
        List<PurchaseSpendBreakdownItem> warehouseBreakdown,
        long pendingApprovalCount,
        long approvedCount,
        long partiallyReceivedCount,
        long fullyReceivedCount,
        long overdueCount,
        long cancelledCount,
        long rejectedCount) {

    @JsonProperty("spendBySupplier")
    public List<PurchaseSpendBreakdownItem> spendBySupplier() {
        return supplierBreakdown;
    }

    @JsonProperty("spendByWarehouse")
    public List<PurchaseSpendBreakdownItem> spendByWarehouse() {
        return warehouseBreakdown;
    }
}
