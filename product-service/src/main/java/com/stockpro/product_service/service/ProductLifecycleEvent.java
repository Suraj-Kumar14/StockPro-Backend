package com.stockpro.product_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;

@Builder
public record ProductLifecycleEvent(
        UUID eventId,
        String eventType,
        Long productId,
        String sku,
        String name,
        String category,
        String brand,
        String barcode,
        Integer reorderLevel,
        Integer maxStockLevel,
        Integer leadTimeDays,
        Boolean isActive,
        Long actorId,
        LocalDateTime eventTime,
        String oldValue,
        String newValue) {
}
