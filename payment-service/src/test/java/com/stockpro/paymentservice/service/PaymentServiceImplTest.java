package com.stockpro.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.paymentservice.client.PurchaseOrderLookupResponse;
import com.stockpro.paymentservice.client.PurchaseServiceClient;
import com.stockpro.paymentservice.client.SupplierLookupResponse;
import com.stockpro.paymentservice.client.SupplierServiceClient;
import com.stockpro.paymentservice.dto.request.ApprovePaymentRequest;
import com.stockpro.paymentservice.dto.request.CreatePaymentRequest;
import com.stockpro.paymentservice.dto.request.MarkPaymentPaidRequest;
import com.stockpro.paymentservice.dto.request.RejectPaymentRequest;
import com.stockpro.paymentservice.dto.request.ReversePaymentRequest;
import com.stockpro.paymentservice.dto.request.SubmitPaymentRequest;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.PaymentSummaryResponse;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.exception.InvalidPaymentStateException;
import com.stockpro.paymentservice.exception.PaymentValidationException;
import com.stockpro.paymentservice.mapper.PaymentMapper;
import com.stockpro.paymentservice.rabbitmq.PaymentEventPublisher;
import com.stockpro.paymentservice.repository.PaymentHistoryRepository;
import com.stockpro.paymentservice.repository.PaymentRepository;
import com.stockpro.paymentservice.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private PurchaseServiceClient purchaseServiceClient;

    @Mock
    private SupplierServiceClient supplierServiceClient;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private final PaymentMapper paymentMapper = new PaymentMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PurchaseOrderLookupResponse purchaseOrder;
    private SupplierLookupResponse supplier;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                paymentHistoryRepository,
                purchaseServiceClient,
                supplierServiceClient,
                paymentMapper,
                paymentEventPublisher);

        purchaseOrder = new PurchaseOrderLookupResponse();
        purchaseOrder.setPurchaseOrderId(101L);
        purchaseOrder.setPoId(101L);
        purchaseOrder.setPoNumber("PO-101");
        purchaseOrder.setSupplierId(7L);
        purchaseOrder.setSupplierName("Acme Supplies");
        purchaseOrder.setStatus("RECEIVED");
        purchaseOrder.setTotalAmount(new BigDecimal("1000.00"));

        supplier = new SupplierLookupResponse();
        supplier.setSupplierId(7L);
        supplier.setName("Acme Supplies");
        supplier.setIsActive(true);
        supplier.setStatus("ACTIVE");
    }

    @Test
    void createPayment_shouldCreateDraftPayment_whenValidRequest() {
        when(purchaseServiceClient.getPurchaseOrder(101L)).thenReturn(purchaseOrder);
        when(supplierServiceClient.getSupplier(7L)).thenReturn(supplier);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.existsByPaymentNumber(any())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(1L);
            return payment;
        });

        PaymentResponse response = paymentService.createPayment(new CreatePaymentRequest(
                101L, null, new BigDecimal("400"), PaymentMethod.NEFT, LocalDate.of(2026, 5, 1),
                "TXN-1", "BANK-1", "Initial draft"), 11L);

        assertEquals(PaymentStatus.DRAFT, response.status());
        assertEquals(new BigDecimal("600.00"), response.remainingAmount());
        verify(paymentEventPublisher).publish(eq("payment.created"), any());
    }

    @Test
    void createPayment_shouldRejectAmountGreaterThanRemainingAmount() {
        when(purchaseServiceClient.getPurchaseOrder(101L)).thenReturn(purchaseOrder);
        when(supplierServiceClient.getSupplier(7L)).thenReturn(supplier);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);

        assertThrows(PaymentValidationException.class, () -> paymentService.createPayment(
                new CreatePaymentRequest(101L, null, new BigDecimal("1400"), PaymentMethod.NEFT, LocalDate.now(), null, null, null),
                11L));
    }

    @Test
    void submitApproveAndMarkPaid_shouldDriveLifecycle() {
        Payment draft = payment();
        draft.setStatus(PaymentStatus.DRAFT);

        when(paymentRepository.findByPaymentId(1L)).thenReturn(Optional.of(draft));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseServiceClient.getPurchaseOrder(101L)).thenReturn(purchaseOrder);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);

        PaymentResponse pending = paymentService.submitPayment(1L, new SubmitPaymentRequest("submit"), 11L);
        assertEquals(PaymentStatus.PENDING_APPROVAL, pending.status());

        PaymentResponse approved = paymentService.approvePayment(1L, new ApprovePaymentRequest("approved"), 12L);
        assertEquals(PaymentStatus.APPROVED, approved.status());

        PaymentResponse paid = paymentService.markPaymentPaid(1L, new MarkPaymentPaidRequest(
                PaymentMethod.RTGS, LocalDate.of(2026, 5, 2), "TXN-2", "BANK-2", "cleared"), 12L);
        assertEquals(PaymentStatus.PAID, paid.status());
        verify(paymentEventPublisher).publish(eq("payment.submitted"), any());
        verify(paymentEventPublisher).publish(eq("payment.approved"), any());
        verify(paymentEventPublisher).publish(eq("payment.paid"), any());
    }

    @Test
    void markPaymentPaid_shouldMarkPartialPaymentCorrectly() {
        Payment approved = payment();
        approved.setStatus(PaymentStatus.APPROVED);
        approved.setPaymentAmount(new BigDecimal("300.00"));

        when(paymentRepository.findByPaymentId(1L)).thenReturn(Optional.of(approved));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseServiceClient.getPurchaseOrder(101L)).thenReturn(purchaseOrder);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("200.00"));

        PaymentResponse response = paymentService.markPaymentPaid(1L, new MarkPaymentPaidRequest(
                PaymentMethod.NEFT, LocalDate.of(2026, 5, 3), "TXN-3", null, "partial"), 15L);

        assertEquals(PaymentStatus.PARTIALLY_PAID, response.status());
        assertEquals(new BigDecimal("500.00"), response.remainingAmount());
        verify(paymentEventPublisher).publish(eq("payment.partially-paid"), any());
    }

    @Test
    void rejectPayment_shouldRequireReason() {
        assertThrows(PaymentValidationException.class, () -> paymentService.rejectPayment(1L, new RejectPaymentRequest(" "), 12L));
    }

    @Test
    void reversePayment_shouldRequirePaidStatus() {
        Payment approved = payment();
        approved.setStatus(PaymentStatus.APPROVED);
        when(paymentRepository.findByPaymentId(1L)).thenReturn(Optional.of(approved));

        assertThrows(InvalidPaymentStateException.class,
                () -> paymentService.reversePayment(1L, new ReversePaymentRequest("undo"), 1L));
        verify(paymentEventPublisher, never()).publish(eq("payment.reversed"), any());
    }

    @Test
    void reversePayment_shouldReversePaidPayment() {
        Payment paid = payment();
        paid.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByPaymentId(1L)).thenReturn(Optional.of(paid));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.reversePayment(1L, new ReversePaymentRequest("duplicate settlement"), 1L);

        assertEquals(PaymentStatus.REVERSED, response.status());
        verify(paymentEventPublisher).publish(eq("payment.reversed"), any());
    }

    @Test
    void getPaidAmountForPurchaseOrder_shouldReturnScaledAmount() {
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("120.5"));
        assertEquals(new BigDecimal("120.50"), paymentService.getPaidAmountForPurchaseOrder(101L));
    }

    @Test
    void getPaymentSummary_shouldReturnZeroes_whenNoPaymentsExist() {
        when(paymentRepository.sumPaymentAmountByStatusIn(any())).thenReturn(null);
        when(paymentRepository.sumRemainingAmountExcludingStatuses(any())).thenReturn(null);

        PaymentSummaryResponse response = paymentService.getPaymentSummary();

        assertEquals(0L, response.totalPayments());
        assertEquals(0L, response.pendingApprovalCount());
        assertEquals(new BigDecimal("0.00"), response.totalPaidAmount());
        assertEquals(new BigDecimal("0.00"), response.pendingPaymentAmount());
        assertEquals(new BigDecimal("0.00"), response.remainingPaymentAmount());
        verify(paymentRepository, never()).findAll();
    }

    @Test
    void getPaymentSummary_shouldUseRepositoryAggregates() {
        when(paymentRepository.count()).thenReturn(4L);
        when(paymentRepository.countByStatus(any())).thenAnswer(invocation -> {
            PaymentStatus status = invocation.getArgument(0);
            return switch (status) {
                case DRAFT, PENDING_APPROVAL -> 1L;
                case PAID -> 2L;
                default -> 0L;
            };
        });
        when(paymentRepository.sumPaymentAmountByStatusIn(any()))
                .thenReturn(new BigDecimal("2500.5"), new BigDecimal("700"));
        when(paymentRepository.sumRemainingAmountExcludingStatuses(any())).thenReturn(new BigDecimal("120.2"));

        PaymentSummaryResponse response = paymentService.getPaymentSummary();

        assertEquals(4L, response.totalPayments());
        assertEquals(1L, response.draftCount());
        assertEquals(1L, response.pendingApprovalCount());
        assertEquals(2L, response.paidCount());
        assertEquals(new BigDecimal("2500.50"), response.totalPaidAmount());
        assertEquals(new BigDecimal("700.00"), response.pendingPaymentAmount());
        assertEquals(new BigDecimal("120.20"), response.remainingPaymentAmount());
        verify(paymentRepository, never()).findAll();
    }

    private Payment payment() {
        return Payment.builder()
                .paymentId(1L)
                .paymentNumber("PAY-20260501-000001")
                .purchaseOrderId(101L)
                .poNumber("PO-101")
                .supplierId(7L)
                .supplierName("Acme Supplies")
                .status(PaymentStatus.DRAFT)
                .paymentMethod(PaymentMethod.NEFT)
                .paymentAmount(new BigDecimal("1000.00"))
                .poTotalAmount(new BigDecimal("1000.00"))
                .previouslyPaidAmount(BigDecimal.ZERO.setScale(2))
                .remainingAmount(BigDecimal.ZERO.setScale(2))
                .currency("INR")
                .history(new ArrayList<>())
                .build();
    }
}
