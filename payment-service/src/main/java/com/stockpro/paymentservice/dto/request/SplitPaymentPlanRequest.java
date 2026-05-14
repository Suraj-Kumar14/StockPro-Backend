package com.stockpro.paymentservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SplitPaymentPlanRequest(
        @NotNull(message = "Purchase order ID is required")
        Long purchaseOrderId,
        @NotNull(message = "Requested amount is required")
        @DecimalMin(value = "0.01", message = "Requested amount must be greater than 0")
        BigDecimal requestedAmount
) {
}
