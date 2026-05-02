package com.stockpro.purchaseservice.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseEvent(
        String eventId,
        String eventType,
        Long purchaseOrderId,
        String poNumber,
        Long supplierId,
        Long warehouseId,
        String status,
        String oldStatus,
        String newStatus,
        BigDecimal totalAmount,
        Long actorId,
        LocalDateTime eventTime,
        String reason,
        List<PurchaseEventLineItem> lineItems,
        Object oldValue,
        Object newValue) {
}
