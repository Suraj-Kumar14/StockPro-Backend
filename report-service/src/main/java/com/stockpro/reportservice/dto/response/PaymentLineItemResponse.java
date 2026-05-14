package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentLineItemResponse(
        Long paymentId,
        String paymentNumber,
        BigDecimal paymentAmount,
        String paymentMethod,
        String razorpayOrderId,
        String razorpayPaymentId,
        String status,
        LocalDate paymentDate,
        LocalDateTime paidAt,
        Long paidBy) {
}
