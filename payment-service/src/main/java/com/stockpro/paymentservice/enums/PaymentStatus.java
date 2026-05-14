package com.stockpro.paymentservice.enums;

public enum PaymentStatus {
    DRAFT,
    INITIATED,
    PENDING_APPROVAL,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    FAILED,
    CANCELLED,
    REJECTED,
    REVERSED
}
