package com.stockpro.movementservice.events;

import java.time.LocalDateTime;

public record WarehouseStockEvent(
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
