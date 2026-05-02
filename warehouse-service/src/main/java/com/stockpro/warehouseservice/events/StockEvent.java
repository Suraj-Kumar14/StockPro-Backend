package com.stockpro.warehouseservice.events;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record StockEvent(
        String eventId,
        String eventType,
        Long stockId,
        Long warehouseId,
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer operationQuantity,
        Long sourceWarehouseId,
        Long destinationWarehouseId,
        String referenceId,
        String referenceType,
        String reason,
        String notes,
        Long actorId,
        LocalDateTime eventTime,
        Integer balanceAfter,
        Object oldValue,
        Object newValue) {
}
