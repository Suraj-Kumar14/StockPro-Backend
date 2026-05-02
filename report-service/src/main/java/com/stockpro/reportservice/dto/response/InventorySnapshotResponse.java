package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventorySnapshotResponse(
        Long snapshotId,
        LocalDate snapshotDate,
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        BigDecimal unitCost,
        BigDecimal totalValue,
        LocalDateTime createdAt) {
}
