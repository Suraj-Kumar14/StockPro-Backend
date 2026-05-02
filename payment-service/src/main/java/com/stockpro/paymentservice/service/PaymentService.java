package com.stockpro.paymentservice.service;

import com.stockpro.paymentservice.dto.request.ApprovePaymentRequest;
import com.stockpro.paymentservice.dto.request.CancelPaymentRequest;
import com.stockpro.paymentservice.dto.request.CreatePaymentRequest;
import com.stockpro.paymentservice.dto.request.MarkPaymentPaidRequest;
import com.stockpro.paymentservice.dto.request.PaymentSearchRequest;
import com.stockpro.paymentservice.dto.request.RejectPaymentRequest;
import com.stockpro.paymentservice.dto.request.ReversePaymentRequest;
import com.stockpro.paymentservice.dto.request.SubmitPaymentRequest;
import com.stockpro.paymentservice.dto.request.UpdatePaymentRequest;
import com.stockpro.paymentservice.dto.response.PaymentAnalyticsResponse;
import com.stockpro.paymentservice.dto.response.PaymentHistoryResponse;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.PaymentSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public interface PaymentService {
    PaymentResponse createPayment(CreatePaymentRequest request, Long actorId);
    PaymentResponse updatePayment(Long paymentId, UpdatePaymentRequest request, Long actorId);
    PaymentResponse getPaymentById(Long paymentId);
    PaymentResponse getPaymentByNumber(String paymentNumber);
    Page<PaymentResponse> getAllPayments(int page, int size, String sortBy, String sortDir);
    Page<PaymentResponse> searchPayments(PaymentSearchRequest request);
    Page<PaymentResponse> getPaymentsByPurchaseOrder(Long purchaseOrderId, int page, int size);
    Page<PaymentResponse> getPaymentsBySupplier(Long supplierId, int page, int size);
    PaymentResponse submitPayment(Long paymentId, SubmitPaymentRequest request, Long actorId);
    PaymentResponse approvePayment(Long paymentId, ApprovePaymentRequest request, Long actorId);
    PaymentResponse rejectPayment(Long paymentId, RejectPaymentRequest request, Long actorId);
    PaymentResponse cancelPayment(Long paymentId, CancelPaymentRequest request, Long actorId);
    PaymentResponse markPaymentPaid(Long paymentId, MarkPaymentPaidRequest request, Long actorId);
    PaymentResponse reversePayment(Long paymentId, ReversePaymentRequest request, Long actorId);
    List<PaymentHistoryResponse> getPaymentHistory(Long paymentId);
    PaymentSummaryResponse getPaymentSummary();
    PaymentAnalyticsResponse getPaymentAnalytics(LocalDate fromDate, LocalDate toDate);
    BigDecimal getPaidAmountForPurchaseOrder(Long purchaseOrderId);
    BigDecimal getRemainingAmountForPurchaseOrder(Long purchaseOrderId);
}
