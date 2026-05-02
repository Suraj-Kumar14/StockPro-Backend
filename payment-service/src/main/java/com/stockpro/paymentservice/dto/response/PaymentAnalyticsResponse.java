package com.stockpro.paymentservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record PaymentAnalyticsResponse(
        BigDecimal totalPaid,
        BigDecimal totalPending,
        Map<String, BigDecimal> monthlyPaidTrend,
        Map<String, Long> paymentsByMethod,
        Map<String, BigDecimal> paymentsBySupplier,
        long pendingApprovals,
        List<String> topPaidSuppliers) {
}
