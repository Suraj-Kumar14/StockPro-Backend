package com.stockpro.paymentservice.client;

public interface PurchaseServiceClient {
    /**
     * Looks up a purchase order without JWT propagation (used by existing manual payment flow).
     */
    PurchaseOrderLookupResponse getPurchaseOrder(Long purchaseOrderId);

    /**
     * Looks up a purchase order while propagating the caller's JWT token (used by Razorpay flow).
     */
    PurchaseOrderLookupResponse getPurchaseOrder(Long purchaseOrderId, String authToken);

    void markPaymentInitiated(Long purchaseOrderId, PaymentTransitionRequest request, String authToken);

    void markPaymentCompleted(Long purchaseOrderId, PaymentTransitionRequest request, String authToken);
}
