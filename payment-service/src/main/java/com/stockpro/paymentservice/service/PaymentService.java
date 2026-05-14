package com.stockpro.paymentservice.service;

import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.PaymentSummaryResponse;
import com.stockpro.paymentservice.enums.PaymentStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PaymentService {
    PaymentResponse getPaymentById(Long paymentId);
    Page<PaymentResponse> getAllPayments(int page, int size, String sortBy, String sortDir);
    Page<PaymentResponse> searchPayments(Long supplierId, PaymentStatus status, LocalDate fromDate, LocalDate toDate,
                                         int page, int size, String sortBy, String sortDir);
    Page<PaymentResponse> getPaymentsByPurchaseOrder(Long purchaseOrderId, int page, int size);
    BigDecimal getPaidAmountForPurchaseOrder(Long purchaseOrderId);
    PaymentSummaryResponse getPaymentSummary();
}
