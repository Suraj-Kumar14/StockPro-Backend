package com.stockpro.purchaseservice.dto.response;

import java.math.BigDecimal;

public record PurchaseOrderLineItemResponse(
        Long lineItemId,
        Long productId,
        String productSku,
        String productName,
        Integer orderedQuantity,
        Integer receivedQuantity,
        Integer pendingQuantity,
        BigDecimal unitCost,
        BigDecimal lineTotal,
        String notes) {
}
