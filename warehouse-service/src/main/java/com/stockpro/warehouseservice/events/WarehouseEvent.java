package com.stockpro.warehouseservice.events;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record WarehouseEvent(
        String eventId,
        String eventType,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long managerId,
        Integer capacity,
        Integer usedCapacity,
        Boolean isActive,
        Long actorId,
        LocalDateTime eventTime,
        Object oldValue,
        Object newValue) {
}
