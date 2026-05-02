package com.stockpro.paymentservice.client;

public interface PurchaseServiceClient {
    PurchaseOrderLookupResponse getPurchaseOrder(Long purchaseOrderId);
}
