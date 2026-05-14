package com.stockpro.paymentservice.dto.response;

import java.math.BigDecimal;

public record PaymentSummaryResponse(
        long totalPayments,
        long draftCount,
        long pendingApprovalCount,
        long approvedCount,
        long partiallyPaidCount,
        long paidCount,
        long cancelledCount,
        long rejectedCount,
        long reversedCount,
        BigDecimal totalPaidAmount,
        BigDecimal pendingPaymentAmount,
        BigDecimal remainingPaymentAmount) {
}
