package com.stockpro.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record RazorpayInitiateRequest(
        @NotNull(message = "Purchase order ID is required")
        Long purchaseOrderId
) {
}
