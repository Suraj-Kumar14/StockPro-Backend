package com.stockpro.paymentservice.dto.response;

import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record PaymentResponse(
        Long paymentId,
        String paymentNumber,
        Long purchaseOrderId,
        String poNumber,
        Long supplierId,
        String supplierName,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        BigDecimal paymentAmount,
        BigDecimal poTotalAmount,
        BigDecimal previouslyPaidAmount,
        BigDecimal remainingAmount,
        String currency,
        LocalDate paymentDate,
        String transactionReference,
        String razorpayOrderId,
        String razorpayPaymentId,
        Long createdBy,
        Long paidBy,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
