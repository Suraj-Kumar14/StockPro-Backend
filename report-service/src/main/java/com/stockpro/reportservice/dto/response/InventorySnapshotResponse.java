package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventorySnapshotResponse(
        Long snapshotId,
        LocalDate snapshotDate,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String productName,
        String sku,
        BigDecimal quantity,
        BigDecimal costPrice,
        BigDecimal stockValue,
        LocalDateTime createdAt,
        String warehouseCode,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        BigDecimal unitCost,
        BigDecimal totalValue) {
}
