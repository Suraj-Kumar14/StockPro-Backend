package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentBillingRowResponse(
        Long paymentId,
        String paymentNumber,
        String poNumber,
        String supplierName,
        BigDecimal paymentAmount,
        String paymentMethod,
        String razorpayPaymentId,
        LocalDate paymentDate,
        LocalDateTime paidAt,
        String status,
        Long paidBy,
        Long approvedBy) {
}
