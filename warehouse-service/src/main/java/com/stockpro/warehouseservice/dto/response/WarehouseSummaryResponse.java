package com.stockpro.warehouseservice.dto.response;

import lombok.Builder;

@Builder
public record WarehouseSummaryResponse(
        long totalWarehouses,
        long activeWarehouses,
        long inactiveWarehouses,
        long totalCapacity,
        long usedCapacity,
        long availableCapacity,
        double averageUtilizationPercentage) {
}
