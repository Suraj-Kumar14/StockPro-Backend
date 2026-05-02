package com.stockpro.purchaseservice.events;

import java.math.BigDecimal;

public record PurchaseEventLineItem(
        Long lineItemId,
        Long productId,
        Integer orderedQuantity,
        Integer receivedQuantity,
        Integer pendingQuantity,
        BigDecimal unitCost) {
}
