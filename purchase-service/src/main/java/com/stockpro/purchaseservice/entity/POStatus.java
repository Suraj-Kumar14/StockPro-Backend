package com.stockpro.purchaseservice.entity;

public enum POStatus {
    DRAFT,
    PENDING_PAYMENT,
    PAYMENT_INITIATED,
    PENDING_APPROVAL,
    APPROVED,
    PAID,
    PARTIALLY_RECEIVED,
    RECEIVED,
    @Deprecated
    FULLY_RECEIVED,
    CANCELLED,
    REJECTED
}
