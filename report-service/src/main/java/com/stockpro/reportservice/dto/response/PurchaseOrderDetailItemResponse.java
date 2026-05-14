package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record PurchaseOrderDetailItemResponse(
        Long productId,
        String productName,
        String sku,
        Integer orderedQuantity,
        Integer receivedQuantity,
        Integer pendingQuantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
