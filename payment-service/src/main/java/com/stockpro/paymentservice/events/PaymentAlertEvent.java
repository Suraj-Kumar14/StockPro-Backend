package com.stockpro.paymentservice.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentAlertEvent(
        String eventId,
        String eventType,
        Long paymentId,
        String paymentNumber,
        String paymentReference,
        Long purchaseOrderId,
        String purchaseOrderNumber,
        Long supplierId,
        String supplierName,
        Long actorId,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String currency,
        String message,
        String actionUrl,
        String sourceService,
        String correlationId,
        LocalDateTime eventTime) {
}
