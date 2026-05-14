package com.stockpro.paymentservice.client;

import java.time.LocalDateTime;

public record PaymentTransitionRequest(
        String paymentStatus,
        Long paymentId,
        String paymentNumber,
        String razorpayOrderId,
        String razorpayPaymentId,
        LocalDateTime paidAt,
        Long actorId) {
}
