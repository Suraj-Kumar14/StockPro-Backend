package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementReportItem(
        Long movementId,
        String movementNumber,
        Long productId,
        String sku,
        String productName,
        Long warehouseId,
        String warehouseName,
        String movementType,
        String direction,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalValue,
        String referenceType,
        String referenceNumber,
        Long performedBy,
        LocalDateTime movementDate) {
}
