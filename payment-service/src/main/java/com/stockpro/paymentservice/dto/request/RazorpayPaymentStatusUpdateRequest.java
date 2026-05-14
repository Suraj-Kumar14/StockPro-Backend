package com.stockpro.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RazorpayPaymentStatusUpdateRequest(
        @NotBlank(message = "razorpayOrderId is required")
        String razorpayOrderId,
        String razorpayPaymentId,
        String failureReason) {
}
