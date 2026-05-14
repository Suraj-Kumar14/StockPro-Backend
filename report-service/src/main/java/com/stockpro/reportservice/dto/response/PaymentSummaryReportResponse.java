package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PaymentSummaryReportResponse(
        long totalPayments,
        BigDecimal totalPaidAmount,
        BigDecimal pendingAmount,
        BigDecimal razorpayAmount,
        Map<String, Long> statusBreakdown,
        Map<String, Long> methodBreakdown,
        List<PaymentSupplierBreakdownItem> supplierBreakdown,
        List<String> warnings) {
}
