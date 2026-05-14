package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancelledPurchaseOrderRowResponse(
        Long purchaseOrderId,
        String poNumber,
        String supplierName,
        String warehouseName,
        BigDecimal totalAmount,
        Long actionedBy,
        String actionedByName,
        LocalDateTime actionedAt,
        String reason) {
}
