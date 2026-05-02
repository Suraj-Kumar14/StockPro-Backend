package com.stockpro.warehouseservice.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record WarehouseResponse(
        Long warehouseId,
        String name,
        String code,
        String location,
        String address,
        Long managerId,
        Integer capacity,
        Integer usedCapacity,
        Integer availableCapacity,
        Double utilizationPercentage,
        Boolean isActive,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
