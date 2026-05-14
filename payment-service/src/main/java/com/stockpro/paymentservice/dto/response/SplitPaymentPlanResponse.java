package com.stockpro.paymentservice.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record SplitPaymentPlanResponse(
        Long purchaseOrderId,
        BigDecimal totalAmount,
        BigDecimal requestedAmount,
        BigDecimal remainingAmount,
        BigDecimal maxAllowedAmount,
        List<BigDecimal> suggestedSplits
) {
}
