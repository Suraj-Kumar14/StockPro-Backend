package com.stockpro.paymentservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RazorpayInitiateRequest(
        @NotNull(message = "Purchase order ID is required")
        Long purchaseOrderId,
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
        BigDecimal paymentAmount
) {
}
