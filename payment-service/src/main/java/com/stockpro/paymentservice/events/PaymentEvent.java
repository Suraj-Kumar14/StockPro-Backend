package com.stockpro.paymentservice.events;

import com.stockpro.paymentservice.enums.PaymentEventType;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentEvent(
        String eventId,
        PaymentEventType eventType,
        Long paymentId,
        String paymentNumber,
        Long purchaseOrderId,
        String poNumber,
        Long supplierId,
        String supplierName,
        PaymentStatus status,
        PaymentStatus oldStatus,
        PaymentStatus newStatus,
        BigDecimal paymentAmount,
        BigDecimal remainingAmount,
        PaymentMethod paymentMethod,
        Long actorId,
        LocalDateTime eventTime,
        String reason,
        String oldValue,
        String newValue) {
}
