package com.stockpro.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.razorpay.RazorpayException;
import com.stockpro.paymentservice.client.PaymentTransitionRequest;
import com.stockpro.paymentservice.client.PurchaseOrderLookupResponse;
import com.stockpro.paymentservice.client.PurchaseServiceClient;
import com.stockpro.paymentservice.dto.request.RazorpayInitiateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayPaymentStatusUpdateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayVerifyRequest;
import com.stockpro.paymentservice.dto.request.SplitPaymentPlanRequest;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.exception.DuplicatePaymentException;
import com.stockpro.paymentservice.exception.InvalidPaymentRequestException;
import com.stockpro.paymentservice.exception.PaymentLimitExceededException;
import com.stockpro.paymentservice.exception.PaymentNotFoundException;
import com.stockpro.paymentservice.exception.PaymentValidationException;
import com.stockpro.paymentservice.exception.RazorpayIntegrationException;
import com.stockpro.paymentservice.mapper.PaymentMapper;
import com.stockpro.paymentservice.publisher.PaymentAlertPublisher;
import com.stockpro.paymentservice.repository.PaymentRepository;
import com.stockpro.paymentservice.service.impl.RazorpayPaymentServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PurchaseServiceClient purchaseServiceClient;

    @Mock
    private PaymentAlertPublisher paymentAlertPublisher;

    private RazorpayPaymentServiceImpl razorpayPaymentService;

    @BeforeEach
    void setUp() {
        razorpayPaymentService = new RazorpayPaymentServiceImpl(
                paymentRepository,
                purchaseServiceClient,
                new PaymentMapper(),
                paymentAlertPublisher);
        ReflectionTestUtils.setField(razorpayPaymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(razorpayPaymentService, "razorpayKeySecret", "rzp_test_secret");
        ReflectionTestUtils.setField(razorpayPaymentService, "razorpayMaxTransactionAmount", new BigDecimal("500000.00"));
    }

    @Test
    void initiatePayment_shouldCreatePendingPaymentAndReturnOrderResponse() throws Exception {
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(101L, null);
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("1000.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("250.00"));
        when(paymentRepository.existsByPaymentNumber(any())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setPaymentId(99L);
            return saved;
        });

        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.get("id")).thenReturn("order_rzp_101");
        OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
        when(orderClient.create(any(JSONObject.class))).thenReturn(order);

        try (MockedConstruction<RazorpayClient> mocked = org.mockito.Mockito.mockConstruction(
                RazorpayClient.class,
                (mock, context) -> ReflectionTestUtils.setField(mock, "orders", orderClient))) {
            var response = razorpayPaymentService.initiatePayment(request, 55L, "Bearer token", false);

            assertEquals("order_rzp_101", response.getRazorpayOrderId());
            assertEquals(new BigDecimal("750.00"), response.getAmount());
            assertEquals("rzp_test_key", response.getKeyId());
            assertEquals(1, mocked.constructed().size());
        }

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.INITIATED, savedPayment.getStatus());
        assertEquals(PaymentMethod.RAZORPAY, savedPayment.getPaymentMethod());
        assertEquals(new BigDecimal("750.00"), savedPayment.getPaymentAmount());
        assertEquals(new BigDecimal("250.00"), savedPayment.getPreviouslyPaidAmount());
        assertEquals(new BigDecimal("0.00"), savedPayment.getRemainingAmount());
        assertNotNull(savedPayment.getPaymentNumber());

        verify(purchaseServiceClient).markPaymentInitiated(eq(101L), any(PaymentTransitionRequest.class), eq("Bearer token"));
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.pending"), any());
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.initiated"), any());
    }

    @Test
    void initiatePayment_shouldRejectNullPurchaseOrderId() {
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(null, null);

        InvalidPaymentRequestException exception = assertThrows(
                InvalidPaymentRequestException.class,
                () -> razorpayPaymentService.initiatePayment(request, 1L, "Bearer token", false));

        assertEquals("Invalid or missing purchaseOrderId", exception.getMessage());
        verify(purchaseServiceClient, never()).getPurchaseOrder(any(), any());
    }

    @Test
    void initiatePayment_shouldRejectInvalidPurchaseOrderStatus() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "APPROVED", new BigDecimal("1000.00"));
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(101L, null);
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);

        PaymentValidationException exception = assertThrows(
                PaymentValidationException.class,
                () -> razorpayPaymentService.initiatePayment(request, 1L, "Bearer token", false));

        assertEquals("Cannot initiate payment for a PO with status: APPROVED. Only purchase orders pending payment can be paid.",
                exception.getMessage());
    }

    @Test
    void initiatePayment_shouldRejectDuplicateOrFullyPaidPurchaseOrder() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PAYMENT_INITIATED", new BigDecimal("500.00"));
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(101L, null);
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("500.00"));

        assertThrows(DuplicatePaymentException.class,
                () -> razorpayPaymentService.initiatePayment(request, 1L, "Bearer token", false));
    }

    @Test
    void initiatePayment_shouldRecoverIncorrectlyPaidPurchaseOrderWhenBalanceRemains() throws Exception {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PAID", new BigDecimal("1000.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("250.00"));
        when(paymentRepository.existsByPaymentNumber(any())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setPaymentId(99L);
            return saved;
        });

        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.get("id")).thenReturn("order_rzp_101");
        OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
        when(orderClient.create(any(JSONObject.class))).thenReturn(order);

        try (MockedConstruction<RazorpayClient> mocked = org.mockito.Mockito.mockConstruction(
                RazorpayClient.class,
                (mock, context) -> ReflectionTestUtils.setField(mock, "orders", orderClient))) {
            var response = razorpayPaymentService.initiatePayment(
                    new RazorpayInitiateRequest(101L, null),
                    55L,
                    "Bearer token",
                    false);

            assertEquals(new BigDecimal("750.00"), response.getAmount());
            assertEquals(1, mocked.constructed().size());
        }

        ArgumentCaptor<PaymentTransitionRequest> transitionCaptor = ArgumentCaptor.forClass(PaymentTransitionRequest.class);
        verify(purchaseServiceClient).markPaymentInitiated(eq(101L), transitionCaptor.capture(), eq("Bearer token"));
        assertEquals("INITIATED", transitionCaptor.getValue().paymentStatus());
    }

    @Test
    void initiatePayment_shouldRejectConfiguredLimitBeforeCallingRazorpay() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("600000.00"));
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(101L, null);
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);

        PaymentLimitExceededException exception = assertThrows(
                PaymentLimitExceededException.class,
                () -> razorpayPaymentService.initiatePayment(request, 1L, "Bearer token", false));

        assertEquals("Payment amount exceeds Razorpay transaction limit. Please split the payment or contact admin.", exception.getMessage());
        assertEquals(new BigDecimal("600000.00"), exception.getRequestedAmount());
        assertEquals(new BigDecimal("500000.00"), exception.getMaxAllowedAmount());
        assertEquals(new BigDecimal("600000.00"), exception.getRemainingAmount());
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.limit_exceeded"), any());
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.split_recommended"), any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void initiatePayment_shouldAllowAuthorizedSplitAmountWithinConfiguredLimit() throws Exception {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("600000.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.existsByPaymentNumber(any())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setPaymentId(99L);
            return saved;
        });

        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.get("id")).thenReturn("order_rzp_101");
        OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
        when(orderClient.create(any(JSONObject.class))).thenReturn(order);

        try (MockedConstruction<RazorpayClient> mocked = org.mockito.Mockito.mockConstruction(
                RazorpayClient.class,
                (mock, context) -> ReflectionTestUtils.setField(mock, "orders", orderClient))) {
            var response = razorpayPaymentService.initiatePayment(
                    new RazorpayInitiateRequest(101L, new BigDecimal("500000.00")),
                    55L,
                    "Bearer token",
                    true);

            assertEquals(new BigDecimal("500000.00"), response.getAmount());
            assertEquals(1, mocked.constructed().size());
        }
    }

    @Test
    void initiatePayment_shouldRejectSplitAmountAboveRemainingAmount() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("620000.00"));
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(101L, new BigDecimal("150000.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("500000.00"));

        InvalidPaymentRequestException exception = assertThrows(
                InvalidPaymentRequestException.class,
                () -> razorpayPaymentService.initiatePayment(request, 55L, "Bearer token", true));

        assertEquals("Payment amount cannot be greater than remaining amount", exception.getMessage());
    }

    @Test
    void initiatePayment_shouldRejectSplitAmountAboveConfiguredLimit() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("620000.00"));
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(101L, new BigDecimal("500001.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);

        PaymentLimitExceededException exception = assertThrows(
                PaymentLimitExceededException.class,
                () -> razorpayPaymentService.initiatePayment(request, 55L, "Bearer token", true));

        assertEquals(new BigDecimal("500001.00"), exception.getRequestedAmount());
    }

    @Test
    void initiatePayment_shouldCreateRazorpayOrderForSecondSplitAmount() throws Exception {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PAYMENT_INITIATED", new BigDecimal("620000.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("500000.00"));
        when(paymentRepository.existsByPaymentNumber(any())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setPaymentId(100L);
            return saved;
        });

        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.get("id")).thenReturn("order_rzp_102");
        OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
        when(orderClient.create(any(JSONObject.class))).thenReturn(order);

        try (MockedConstruction<RazorpayClient> mocked = org.mockito.Mockito.mockConstruction(
                RazorpayClient.class,
                (mock, context) -> ReflectionTestUtils.setField(mock, "orders", orderClient))) {
            var response = razorpayPaymentService.initiatePayment(
                    new RazorpayInitiateRequest(101L, new BigDecimal("120000.00")),
                    55L,
                    "Bearer token",
                    true);

            assertEquals(new BigDecimal("120000.00"), response.getAmount());
            assertEquals(1, mocked.constructed().size());
        }
    }

    @Test
    void verifyPayment_shouldMarkPartialPaymentAndNotifyPurchaseService() {
        Payment pendingPayment = pendingPayment();
        when(paymentRepository.findByRazorpayOrderId("order_rzp_101")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<Utils> utils = org.mockito.Mockito.mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), eq("rzp_test_secret"))).thenReturn(true);

            var response = razorpayPaymentService.verifyPayment(
                    new RazorpayVerifyRequest("order_rzp_101", "pay_123", "sig_123"),
                    77L,
                    "Bearer token");

            assertEquals(PaymentStatus.PARTIALLY_PAID, response.status());
            assertEquals("pay_123", response.razorpayPaymentId());
            assertEquals(new BigDecimal("250.00"), response.remainingAmount());
        }

        ArgumentCaptor<PaymentTransitionRequest> transitionCaptor = ArgumentCaptor.forClass(PaymentTransitionRequest.class);
        verify(purchaseServiceClient).markPaymentCompleted(eq(101L), transitionCaptor.capture(), eq("Bearer token"));
        PaymentTransitionRequest transitionRequest = transitionCaptor.getValue();
        assertEquals("PARTIALLY_PAID", transitionRequest.paymentStatus());
        assertEquals("pay_123", transitionRequest.razorpayPaymentId());
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.success"), any());
    }

    @Test
    void verifyPayment_shouldMarkFinalSplitAsPaidWhenRemainingAmountBecomesZero() {
        Payment pendingPayment = pendingPayment();
        pendingPayment.setPaymentAmount(new BigDecimal("250.00"));
        pendingPayment.setPreviouslyPaidAmount(new BigDecimal("750.00"));
        when(paymentRepository.findByRazorpayOrderId("order_rzp_101")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("750.00"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<Utils> utils = org.mockito.Mockito.mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), eq("rzp_test_secret"))).thenReturn(true);

            var response = razorpayPaymentService.verifyPayment(
                    new RazorpayVerifyRequest("order_rzp_101", "pay_123", "sig_123"),
                    77L,
                    "Bearer token");

            assertEquals(PaymentStatus.PAID, response.status());
            assertEquals(new BigDecimal("0.00"), response.remainingAmount());
        }
    }

    @Test
    void verifyPayment_shouldRejectDuplicateVerification() {
        Payment paidPayment = pendingPayment();
        RazorpayVerifyRequest request = new RazorpayVerifyRequest("order_rzp_101", "pay_123", "sig_123");
        paidPayment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByRazorpayOrderId("order_rzp_101")).thenReturn(Optional.of(paidPayment));

        assertThrows(DuplicatePaymentException.class,
                () -> razorpayPaymentService.verifyPayment(request, 77L, "Bearer token"));
    }

    @Test
    void verifyPayment_shouldRejectInvalidSignature() {
        RazorpayVerifyRequest request = new RazorpayVerifyRequest("order_rzp_101", "pay_123", "sig_123");
        when(paymentRepository.findByRazorpayOrderId("order_rzp_101")).thenReturn(Optional.of(pendingPayment()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<Utils> utils = org.mockito.Mockito.mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), eq("rzp_test_secret"))).thenReturn(false);

            assertThrows(RazorpayIntegrationException.class,
                    () -> razorpayPaymentService.verifyPayment(request, 77L, "Bearer token"));
        }
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.failed"), any());
    }

    @Test
    void verifyPayment_shouldThrowWhenPendingRecordMissing() {
        RazorpayVerifyRequest request = new RazorpayVerifyRequest("order_rzp_404", "pay_404", "sig_404");
        when(paymentRepository.findByRazorpayOrderId("order_rzp_404")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> razorpayPaymentService.verifyPayment(request, 77L, "Bearer token"));
    }

    @Test
    void recordFailedPayment_shouldPersistFailureStatus() {
        Payment pendingPayment = pendingPayment();
        when(paymentRepository.findByRazorpayOrderId("order_rzp_101")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = razorpayPaymentService.recordFailedPayment(
                new RazorpayPaymentStatusUpdateRequest("order_rzp_101", "pay_failed", "Gateway timeout"),
                77L);

        assertEquals(PaymentStatus.FAILED, response.status());
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.failed"), any());
    }

    @Test
    void recordCancelledPayment_shouldPersistCancelledStatus() {
        Payment pendingPayment = pendingPayment();
        when(paymentRepository.findByRazorpayOrderId("order_rzp_101")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = razorpayPaymentService.recordCancelledPayment(
                new RazorpayPaymentStatusUpdateRequest("order_rzp_101", null, "Payment cancelled by user"),
                77L);

        assertEquals(PaymentStatus.CANCELLED, response.status());
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.cancelled"), any());
    }

    @Test
    void getRemainingAmount_shouldReturnScaledValuesAndFloorNegativeRemainder() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("500.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("650.25"));

        var response = razorpayPaymentService.getRemainingAmount(101L, "Bearer token");

        assertEquals(new BigDecimal("500.00"), response.getTotalAmount());
        assertEquals(new BigDecimal("650.25"), response.getPaidAmount());
        assertEquals(new BigDecimal("0.00"), response.getRemainingAmount());
        assertEquals(PaymentStatus.PAID, response.getStatus());
        assertEquals("INR", response.getCurrency());
    }

    @Test
    void getRemainingAmount_shouldReturnPartiallyPaidStatusWhenBalanceStillExists() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PAYMENT_INITIATED", new BigDecimal("620000.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("500000.00"));

        var response = razorpayPaymentService.getRemainingAmount(101L, "Bearer token");

        assertEquals(new BigDecimal("500000.00"), response.getPaidAmount());
        assertEquals(new BigDecimal("120000.00"), response.getRemainingAmount());
        assertEquals(PaymentStatus.PARTIALLY_PAID, response.getStatus());
    }

    @Test
    void initiatePayment_shouldIncrementPaymentNumberWhenCandidateExists() throws Exception {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("100.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.existsByPaymentNumber(any())).thenReturn(true, false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.get("id")).thenReturn("order_rzp_101");
        OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
        when(orderClient.create(any(JSONObject.class))).thenReturn(order);

        try (MockedConstruction<RazorpayClient> mocked = org.mockito.Mockito.mockConstruction(
                RazorpayClient.class,
                (mock, context) -> ReflectionTestUtils.setField(mock, "orders", orderClient))) {
            var response = razorpayPaymentService.initiatePayment(new RazorpayInitiateRequest(101L, null), 55L, "Bearer token", false);

            assertEquals("order_rzp_101", response.getRazorpayOrderId());
            assertEquals(1, mocked.constructed().size());
        }

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(2L, sequenceFromPaymentNumber(paymentCaptor.getValue().getPaymentNumber()));
    }

    @Test
    void initiatePayment_shouldRejectWhenRazorpayCredentialsMissing() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("100.00"));
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(101L, null);
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);
        ReflectionTestUtils.setField(razorpayPaymentService, "razorpayKeyId", "");

        RazorpayIntegrationException exception = assertThrows(
                RazorpayIntegrationException.class,
                () -> razorpayPaymentService.initiatePayment(request, 55L, "Bearer token", false));

        assertEquals("Razorpay credentials not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET env variables.", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void initiatePayment_shouldTranslateRazorpayAmountLimitError() throws Exception {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("100.00"));
        RazorpayInitiateRequest request = new RazorpayInitiateRequest(101L, null);
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);

        OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
        when(orderClient.create(any(JSONObject.class)))
                .thenThrow(new RazorpayException("BAD_REQUEST_ERROR: amount exceeds maximum amount allowed"));

        try (MockedConstruction<RazorpayClient> mocked = org.mockito.Mockito.mockConstruction(
                RazorpayClient.class,
                (mock, context) -> ReflectionTestUtils.setField(mock, "orders", orderClient))) {
            PaymentLimitExceededException exception = assertThrows(
                    PaymentLimitExceededException.class,
                    () -> razorpayPaymentService.initiatePayment(request, 55L, "Bearer token", false));

            assertEquals(new BigDecimal("100.00"), exception.getRequestedAmount());
            assertEquals(new BigDecimal("500000.00"), exception.getMaxAllowedAmount());
            assertEquals(1, mocked.constructed().size());
        }

        verify(paymentAlertPublisher).publish(eq("razorpay.payment.limit_exceeded"), any());
        verify(paymentAlertPublisher).publish(eq("razorpay.payment.split_recommended"), any());
    }

    @Test
    void getSplitPaymentPlan_shouldChunkRequestedAmountByConfiguredLimit() {
        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("600000.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(BigDecimal.ZERO);

        var response = razorpayPaymentService.getSplitPaymentPlan(
                new SplitPaymentPlanRequest(101L, new BigDecimal("600000.00")),
                "Bearer token");

        assertEquals(new BigDecimal("500000.00"), response.maxAllowedAmount());
        assertEquals(List.of(new BigDecimal("500000.00"), new BigDecimal("100000.00")), response.suggestedSplits());
    }

    @Test
    void getSplitPaymentPlan_shouldRejectInvalidRequestValues() {
        SplitPaymentPlanRequest missingPurchaseOrderRequest = new SplitPaymentPlanRequest(null, BigDecimal.TEN);
        InvalidPaymentRequestException invalidPoException = assertThrows(
                InvalidPaymentRequestException.class,
                () -> razorpayPaymentService.getSplitPaymentPlan(missingPurchaseOrderRequest, "Bearer token"));
        assertEquals("Invalid or missing purchaseOrderId", invalidPoException.getMessage());

        PurchaseOrderLookupResponse po = purchaseOrder(101L, "PO-101", "PENDING_PAYMENT", new BigDecimal("600000.00"));
        when(purchaseServiceClient.getPurchaseOrder(101L, "Bearer token")).thenReturn(po);
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(eq(101L), any())).thenReturn(new BigDecimal("100000.00"));

        SplitPaymentPlanRequest zeroAmountRequest = new SplitPaymentPlanRequest(101L, BigDecimal.ZERO);
        InvalidPaymentRequestException invalidAmountException = assertThrows(
                InvalidPaymentRequestException.class,
                () -> razorpayPaymentService.getSplitPaymentPlan(zeroAmountRequest, "Bearer token"));
        assertEquals("Payment amount must be greater than 0", invalidAmountException.getMessage());

        SplitPaymentPlanRequest aboveRemainingRequest = new SplitPaymentPlanRequest(101L, new BigDecimal("550001.00"));
        InvalidPaymentRequestException aboveRemainingException = assertThrows(
                InvalidPaymentRequestException.class,
                () -> razorpayPaymentService.getSplitPaymentPlan(aboveRemainingRequest, "Bearer token"));
        assertEquals("Payment amount cannot be greater than remaining amount", aboveRemainingException.getMessage());
    }

    @Test
    void recordFailedPayment_shouldThrowWhenPaymentMissing() {
        RazorpayPaymentStatusUpdateRequest request =
                new RazorpayPaymentStatusUpdateRequest("missing-order", "pay_missing", "Gateway timeout");
        when(paymentRepository.findByRazorpayOrderId("missing-order")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> razorpayPaymentService.recordFailedPayment(request, 77L));
    }

    private long sequenceFromPaymentNumber(String paymentNumber) {
        return Long.parseLong(paymentNumber.substring(paymentNumber.lastIndexOf('-') + 1));
    }

    private PurchaseOrderLookupResponse purchaseOrder(Long purchaseOrderId, String poNumber, String status, BigDecimal totalAmount) {
        PurchaseOrderLookupResponse po = new PurchaseOrderLookupResponse();
        po.setPoId(purchaseOrderId);
        po.setPurchaseOrderId(purchaseOrderId);
        po.setPoNumber(poNumber);
        po.setSupplierId(7L);
        po.setSupplierName("Acme Supplies");
        po.setStatus(status);
        po.setTotalAmount(totalAmount);
        po.setCreatedBy(11L);
        po.setCreatedById(11L);
        return po;
    }

    private Payment pendingPayment() {
        return Payment.builder()
                .paymentId(9L)
                .paymentNumber("PAY-20260509-000001")
                .purchaseOrderId(101L)
                .poNumber("PO-101")
                .supplierId(7L)
                .supplierName("Acme Supplies")
                .status(PaymentStatus.INITIATED)
                .paymentMethod(PaymentMethod.RAZORPAY)
                .paymentAmount(new BigDecimal("750.00"))
                .poTotalAmount(new BigDecimal("1000.00"))
                .previouslyPaidAmount(new BigDecimal("250.00"))
                .remainingAmount(BigDecimal.ZERO.setScale(2))
                .currency("INR")
                .razorpayOrderId("order_rzp_101")
                .createdBy(55L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
