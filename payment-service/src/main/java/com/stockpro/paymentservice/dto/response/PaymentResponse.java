package com.stockpro.paymentservice.dto.response;

import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

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
        String bankReference,
        String remarks,
        String rejectionReason,
        String cancellationReason,
        String reversalReason,
        Long createdBy,
        Long approvedBy,
        Long paidBy,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        LocalDateTime cancelledAt,
        LocalDateTime paidAt,
        LocalDateTime reversedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PaymentHistoryResponse> history) {
}
