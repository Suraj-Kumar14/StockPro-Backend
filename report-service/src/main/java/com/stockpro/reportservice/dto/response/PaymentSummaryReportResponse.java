package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PaymentSummaryReportResponse(
        long totalPayments,
        long paidCount,
        long pendingCount,
        long cancelledCount,
        BigDecimal totalPaidAmount,
        BigDecimal pendingAmount,
        List<SupplierPaymentItem> supplierPayments) {
}
