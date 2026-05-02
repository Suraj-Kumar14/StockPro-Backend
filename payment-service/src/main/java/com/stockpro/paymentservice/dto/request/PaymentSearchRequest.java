package com.stockpro.paymentservice.dto.request;

import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentSearchRequest(
        String keyword,
        Long purchaseOrderId,
        Long supplierId,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        Long createdBy,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        int page,
        int size,
        String sortBy,
        String sortDir) {
}
