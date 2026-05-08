package com.stockpro.paymentservice.service;

import com.stockpro.paymentservice.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentResponse getPaymentById(Long paymentId);
    Page<PaymentResponse> getAllPayments(int page, int size, String sortBy, String sortDir);
    Page<PaymentResponse> getPaymentsByPurchaseOrder(Long purchaseOrderId, int page, int size);
    BigDecimal getPaidAmountForPurchaseOrder(Long purchaseOrderId);
}
