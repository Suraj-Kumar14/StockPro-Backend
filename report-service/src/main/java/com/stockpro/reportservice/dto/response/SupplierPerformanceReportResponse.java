package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record SupplierPerformanceReportResponse(
        Long supplierId,
        String supplierName,
        long totalOrders,
        long receivedOrders,
        long delayedOrders,
        BigDecimal totalSpend,
        BigDecimal averageLeadTimeDays,
        BigDecimal rating) {
}
