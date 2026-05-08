package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.PaymentStatusSnapshotDTO;

public interface PaymentGateway {
    PaymentStatusSnapshotDTO getPaymentStatusSnapshot(Long purchaseOrderId);
}
