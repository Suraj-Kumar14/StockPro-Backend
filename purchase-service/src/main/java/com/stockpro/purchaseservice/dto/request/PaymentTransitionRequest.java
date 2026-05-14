package com.stockpro.purchaseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PaymentTransitionRequest(
        @NotBlank String paymentStatus,
        Long paymentId,
        String paymentNumber,
        String razorpayOrderId,
        String razorpayPaymentId,
        LocalDateTime paidAt,
        @NotNull Long actorId) {
}
