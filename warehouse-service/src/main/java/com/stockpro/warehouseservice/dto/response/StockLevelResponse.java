package com.stockpro.warehouseservice.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record StockLevelResponse(
        Long stockId,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String productName,
        String sku,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        String locationCode,
        LocalDateTime lastUpdated) {
}
