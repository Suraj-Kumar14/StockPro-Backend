package com.stockpro.purchaseservice.entity;

public enum POStatus {
    DRAFT,
    PENDING,
    APPROVED,
    PARTIALLY_RECEIVED,
    RECEIVED,
    @Deprecated
    FULLY_RECEIVED,
    CANCELLED
}
