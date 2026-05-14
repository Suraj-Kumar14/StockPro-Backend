package com.stockpro.paymentservice.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.stockpro.paymentservice.client.PurchaseOrderLookupResponse;
import com.stockpro.paymentservice.client.PaymentTransitionRequest;
import com.stockpro.paymentservice.client.PurchaseServiceClient;
import com.stockpro.paymentservice.dto.request.RazorpayInitiateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayPaymentStatusUpdateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayVerifyRequest;
import com.stockpro.paymentservice.dto.request.SplitPaymentPlanRequest;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.RazorpayOrderResponse;
import com.stockpro.paymentservice.dto.response.RemainingAmountResponse;
import com.stockpro.paymentservice.dto.response.SplitPaymentPlanResponse;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.events.PaymentAlertEvent;
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
import com.stockpro.paymentservice.service.RazorpayPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class RazorpayPaymentServiceImpl implements RazorpayPaymentService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<PaymentStatus> PAID_STATUSES = EnumSet.of(PaymentStatus.PAID, PaymentStatus.PARTIALLY_PAID);
    private static final Set<String> ALLOWED_PO_STATUSES = Set.of("PENDING_PAYMENT", "PAYMENT_INITIATED", "PAID");
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String EVENT_PENDING = "razorpay.payment.pending";
    private static final String EVENT_INITIATED = "razorpay.payment.initiated";
    private static final String EVENT_SUCCESS = "razorpay.payment.success";
    private static final String EVENT_FAILED = "razorpay.payment.failed";
    private static final String EVENT_CANCELLED = "razorpay.payment.cancelled";
    private static final String EVENT_LIMIT_EXCEEDED = "razorpay.payment.limit_exceeded";
    private static final String EVENT_SPLIT_RECOMMENDED = "razorpay.payment.split_recommended";
    private static final String PAYMENTS_ACTION_URL = "/payments";
    private static final String SOURCE_SERVICE = "payment-service";

    private final PaymentRepository paymentRepository;
    private final PurchaseServiceClient purchaseServiceClient;
    private final PaymentMapper paymentMapper;
    private final PaymentAlertPublisher paymentAlertPublisher;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${payment.razorpay.max-transaction-amount:500000}")
    private BigDecimal razorpayMaxTransactionAmount;

    public RazorpayPaymentServiceImpl(PaymentRepository paymentRepository,
                                      PurchaseServiceClient purchaseServiceClient,
                                      PaymentMapper paymentMapper,
                                      PaymentAlertPublisher paymentAlertPublisher) {
        this.paymentRepository = paymentRepository;
        this.purchaseServiceClient = purchaseServiceClient;
        this.paymentMapper = paymentMapper;
        this.paymentAlertPublisher = paymentAlertPublisher;
    }

    @Override
    @Transactional
    public RazorpayOrderResponse initiatePayment(RazorpayInitiateRequest request, Long actorId, String authToken, boolean splitPaymentAllowed) {
        log.info("Initiating Razorpay payment for purchaseOrderId={} actorId={} requestedAmount={} splitPaymentAllowed={}",
                request.purchaseOrderId(), actorId, request.paymentAmount(), splitPaymentAllowed);

        // Defense-in-depth null guard (should be caught by @Valid, but guards against edge cases)
        if (request.purchaseOrderId() == null) {
            throw new InvalidPaymentRequestException("Invalid or missing purchaseOrderId");
        }

        // 1. Fetch purchase order details
        PurchaseOrderLookupResponse po = purchaseServiceClient.getPurchaseOrder(request.purchaseOrderId(), authToken);
        validatePoStatus(po);

        // 3. Calculate amount
        BigDecimal poTotal = scale(po.getTotalAmount());
        BigDecimal paidSoFar = getPaidAmount(request.purchaseOrderId());
        BigDecimal remaining = scale(poTotal.subtract(paidSoFar));

        if (remaining.compareTo(ZERO) <= 0) {
            throw new DuplicatePaymentException(
                    "No remaining amount to pay for purchase order ID " + request.purchaseOrderId());
        }

        BigDecimal requestedAmount = resolveRequestedAmount(request, remaining, splitPaymentAllowed);
        BigDecimal remainingAfterSuccessfulPayment = scale(remaining.subtract(requestedAmount));
        log.info("Validated payment request before Razorpay call: purchaseOrderId={} requestedAmount={} remainingAmount={} maxAllowedAmount={} isSplitPayment={}",
                request.purchaseOrderId(),
                requestedAmount,
                remaining,
                scale(razorpayMaxTransactionAmount),
                request.paymentAmount() != null);
        validateTransactionLimit(requestedAmount, remaining, request.purchaseOrderId(), po.getPoNumber());

        // 4. Create Razorpay order (amount in paise)
        long amountInPaise = requestedAmount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        log.info("Creating Razorpay order: poId={}, poNumber={}, amountRupees={}, amountPaise={}",
                request.purchaseOrderId(), po.getPoNumber(), requestedAmount, amountInPaise);
        String razorpayOrderId = createRazorpayOrder(amountInPaise, request.purchaseOrderId(), po.getPoNumber(), requestedAmount);

        // 5. Persist payment record in INITIATED state while backend verification is still pending.
        String paymentNumber = generatePaymentNumber();
        Long supplierId = po.getSupplierId() != null ? po.getSupplierId() : 0L;
        String supplierName = po.getSupplierName();

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .purchaseOrderId(request.purchaseOrderId())
                .poNumber(po.getPoNumber())
                .supplierId(supplierId)
                .supplierName(supplierName)
                .status(PaymentStatus.INITIATED)
                .paymentMethod(PaymentMethod.RAZORPAY)
                .paymentAmount(requestedAmount)
                .poTotalAmount(poTotal)
                .previouslyPaidAmount(paidSoFar)
                .remainingAmount(remainingAfterSuccessfulPayment.compareTo(ZERO) < 0 ? ZERO : remainingAfterSuccessfulPayment)
                .currency("INR")
                .razorpayOrderId(razorpayOrderId)
                .createdBy(actorId)
                .build();

        Payment savedPendingPayment = paymentRepository.save(payment);
        log.info("Created Razorpay payment record paymentNumber={} razorpayOrderId={}", paymentNumber, razorpayOrderId);
        publishPaymentAlert(EVENT_PENDING, savedPendingPayment, actorId,
                "Razorpay payment is pending for Purchase Order " + safe(po.getPoNumber()) + ".");
        purchaseServiceClient.markPaymentInitiated(request.purchaseOrderId(), new PaymentTransitionRequest(
                savedPendingPayment.getStatus().name(),
                savedPendingPayment.getPaymentId(),
                paymentNumber,
                razorpayOrderId,
                null,
                null,
                actorId), authToken);
        publishPaymentAlert(EVENT_INITIATED, savedPendingPayment, actorId,
                "Razorpay payment initiated for Purchase Order " + safe(po.getPoNumber()) + ".");

        return RazorpayOrderResponse.builder()
                .razorpayOrderId(razorpayOrderId)
                .paymentNumber(paymentNumber)
                .purchaseOrderId(request.purchaseOrderId())
                .amount(requestedAmount)
                .currency("INR")
                .keyId(razorpayKeyId)
                .description("Payment for PO " + (po.getPoNumber() != null ? po.getPoNumber() : request.purchaseOrderId()))
                .build();
    }


    @Override
    @Transactional
    public PaymentResponse verifyPayment(RazorpayVerifyRequest request, Long actorId, String authToken) {
        log.info("Verifying Razorpay payment razorpayOrderId={} razorpayPaymentId={}",
                request.razorpayOrderId(), request.razorpayPaymentId());

        // 1. Find the pending payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for Razorpay order ID: " + request.razorpayOrderId()));

        // 2. Guard against re-verification
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new DuplicatePaymentException(
                    "Payment already verified for Razorpay order ID: " + request.razorpayOrderId());
        }

        // 3. Verify Razorpay signature on backend (never trust frontend)
        try {
            verifyRazorpaySignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
        } catch (RuntimeException ex) {
            Payment failedPayment = updatePaymentOutcome(payment, PaymentStatus.FAILED, request.razorpayPaymentId(), actorId);
            publishPaymentAlert(EVENT_FAILED, failedPayment, actorId,
                    "Razorpay payment failed for Purchase Order " + safe(payment.getPoNumber()) + ". Reason: " + safe(ex.getMessage()) + ".");
            throw ex;
        }

        // 4. Compute the overall PO payment state and persist it on the payment record so
        // downstream consumers do not mistake a successful split payment for a fully paid PO.
        BigDecimal totalPaidAfterVerification = scale(getPaidAmount(payment.getPurchaseOrderId()).add(scale(payment.getPaymentAmount())));
        BigDecimal remainingAfterVerification = scale(payment.getPoTotalAmount().subtract(totalPaidAfterVerification));
        PaymentStatus overallPaymentStatus = remainingAfterVerification.compareTo(ZERO) <= 0
                ? PaymentStatus.PAID
                : PaymentStatus.PARTIALLY_PAID;

        payment.setStatus(overallPaymentStatus);
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setTransactionReference(request.razorpayPaymentId());
        payment.setPaidBy(actorId);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaymentDate(LocalDate.now());
        payment.setRemainingAmount(remainingAfterVerification.compareTo(ZERO) < 0 ? ZERO : remainingAfterVerification);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment verified and marked paymentId={} paymentNumber={} transactionStatus={} overallStatus={} razorpayPaymentId={} remainingAmount={}",
                saved.getPaymentId(), saved.getPaymentNumber(), saved.getStatus(), overallPaymentStatus, request.razorpayPaymentId(), saved.getRemainingAmount());
        purchaseServiceClient.markPaymentCompleted(saved.getPurchaseOrderId(), new PaymentTransitionRequest(
                overallPaymentStatus.name(),
                saved.getPaymentId(),
                saved.getPaymentNumber(),
                saved.getRazorpayOrderId(),
                saved.getRazorpayPaymentId(),
                saved.getPaidAt(),
                actorId), authToken);
        publishPaymentAlert(EVENT_SUCCESS, saved, actorId,
                "Razorpay payment completed successfully for Purchase Order " + safe(saved.getPoNumber()) + ".");

        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse recordFailedPayment(RazorpayPaymentStatusUpdateRequest request, Long actorId) {
        Payment payment = findNonPaidPayment(request.razorpayOrderId());
        Payment failedPayment = updatePaymentOutcome(payment, PaymentStatus.FAILED, request.razorpayPaymentId(), actorId);
        publishPaymentAlert(EVENT_FAILED, failedPayment, actorId,
                "Razorpay payment failed for Purchase Order " + safe(payment.getPoNumber())
                        + ". Reason: " + safe(resolveFailureReason(request.failureReason(), "Payment failed")) + ".");
        return paymentMapper.toResponse(failedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse recordCancelledPayment(RazorpayPaymentStatusUpdateRequest request, Long actorId) {
        Payment payment = findNonPaidPayment(request.razorpayOrderId());
        Payment cancelledPayment = updatePaymentOutcome(payment, PaymentStatus.CANCELLED, request.razorpayPaymentId(), actorId);
        publishPaymentAlert(EVENT_CANCELLED, cancelledPayment, actorId,
                "Razorpay payment cancelled for Purchase Order " + safe(payment.getPoNumber()) + ".");
        return paymentMapper.toResponse(cancelledPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public RemainingAmountResponse getRemainingAmount(Long purchaseOrderId, String authToken) {
        if (purchaseOrderId == null || purchaseOrderId <= 0) {
            throw new InvalidPaymentRequestException("Invalid or missing purchaseOrderId");
        }
        PurchaseOrderLookupResponse po = purchaseServiceClient.getPurchaseOrder(purchaseOrderId, authToken);
        BigDecimal total = scale(po.getTotalAmount());
        BigDecimal paid = getPaidAmount(purchaseOrderId);
        BigDecimal remaining = scale(total.subtract(paid));

        return RemainingAmountResponse.builder()
                .purchaseOrderId(purchaseOrderId)
                .totalAmount(total)
                .paidAmount(paid)
                .remainingAmount(remaining.compareTo(ZERO) < 0 ? ZERO : remaining)
                .status(resolveOverallPaymentStatus(paid, remaining))
                .maxAllowedAmount(scale(razorpayMaxTransactionAmount))
                .currency("INR")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SplitPaymentPlanResponse getSplitPaymentPlan(SplitPaymentPlanRequest request, String authToken) {
        if (request.purchaseOrderId() == null || request.purchaseOrderId() <= 0) {
            throw new InvalidPaymentRequestException("Invalid or missing purchaseOrderId");
        }
        PurchaseOrderLookupResponse po = purchaseServiceClient.getPurchaseOrder(request.purchaseOrderId(), authToken);
        validatePoStatus(po);

        BigDecimal totalAmount = scale(po.getTotalAmount());
        BigDecimal paidAmount = getPaidAmount(request.purchaseOrderId());
        BigDecimal remainingAmount = scale(totalAmount.subtract(paidAmount));
        if (remainingAmount.compareTo(ZERO) <= 0) {
            throw new DuplicatePaymentException("No remaining amount to pay for purchase order ID " + request.purchaseOrderId());
        }

        BigDecimal requestedAmount = scale(request.requestedAmount());
        if (requestedAmount.compareTo(ZERO) <= 0) {
            throw new InvalidPaymentRequestException("Payment amount must be greater than 0");
        }
        if (requestedAmount.compareTo(remainingAmount) > 0) {
            throw new InvalidPaymentRequestException("Payment amount cannot be greater than remaining amount");
        }

        return SplitPaymentPlanResponse.builder()
                .purchaseOrderId(request.purchaseOrderId())
                .totalAmount(totalAmount)
                .requestedAmount(requestedAmount)
                .remainingAmount(remainingAmount)
                .maxAllowedAmount(scale(razorpayMaxTransactionAmount))
                .suggestedSplits(buildSuggestedSplits(requestedAmount))
                .build();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void validatePoStatus(PurchaseOrderLookupResponse po) {
        if (po.getStatus() == null || !ALLOWED_PO_STATUSES.contains(po.getStatus().toUpperCase(Locale.ROOT))) {
            log.warn("Razorpay payment rejected: PO purchaseOrderId={} has status={}",
                    po.getPurchaseOrderId() != null ? po.getPurchaseOrderId() : po.getPoId(), po.getStatus());
            throw new PaymentValidationException(
                    "Cannot initiate payment for a PO with status: " + po.getStatus()
                    + ". Only purchase orders pending payment can be paid.");
        }
        if (po.getTotalAmount() == null || po.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Purchase order total amount is invalid or zero.");
        }
    }

    private String createRazorpayOrder(long amountInPaise, Long purchaseOrderId, String poNumber, BigDecimal amountInRupees) {
        if (razorpayKeyId == null || razorpayKeyId.isBlank()
                || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new RazorpayIntegrationException(
                    "Razorpay credentials not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET env variables.");
        }
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "PO-" + purchaseOrderId);
            orderRequest.put("payment_capture", 1);

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");
            log.info("Razorpay order created orderId={} amount={}", orderId, amountInPaise);
            return orderId;
        } catch (RazorpayException e) {
            String razorpayError = e.getMessage();
            String errorCode = extractRazorpayErrorCode(razorpayError);
            log.error("Razorpay order creation failed: poId={} poNumber={} amountRupees={} amountPaise={} razorpayErrorCode={} message={}",
                    purchaseOrderId, poNumber, amountInRupees, amountInPaise, errorCode, razorpayError);

            if (containsAmountLimitError(razorpayError)) {
                publishLimitExceededAlert(purchaseOrderId, poNumber, amountInRupees);
                throw new PaymentLimitExceededException(
                        "Payment amount exceeds Razorpay transaction limit. Please split the payment or contact admin.",
                        amountInRupees,
                        scale(razorpayMaxTransactionAmount),
                        amountInRupees,
                        true);
            }

            throw new RazorpayIntegrationException("Failed to create Razorpay order: " + razorpayError, e);
        }
    }

    private boolean containsAmountLimitError(String razorpayError) {
        return razorpayError != null
                && razorpayError.toLowerCase(Locale.ROOT).contains("amount exceeds maximum amount allowed");
    }

    private String extractRazorpayErrorCode(String razorpayError) {
        if (razorpayError == null || razorpayError.isBlank()) {
            return "UNKNOWN";
        }

        int separatorIndex = razorpayError.indexOf(':');
        if (separatorIndex <= 0) {
            return "UNKNOWN";
        }

        return razorpayError.substring(0, separatorIndex).trim();
    }

    private void verifyRazorpaySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new RazorpayIntegrationException(
                    "Razorpay key-secret not configured. Cannot verify payment signature.");
        }
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            boolean valid = Utils.verifyPaymentSignature(
                    new JSONObject()
                            .put("razorpay_order_id", razorpayOrderId)
                            .put("razorpay_payment_id", razorpayPaymentId)
                            .put("razorpay_signature", signature),
                    razorpayKeySecret);
            if (!valid) {
                log.warn("Razorpay signature verification FAILED for orderId={} paymentId={}",
                        razorpayOrderId, razorpayPaymentId);
                throw new RazorpayIntegrationException(
                        "Payment signature verification failed. Possible tampering detected.");
            }
            log.info("Razorpay signature verified successfully for orderId={}", razorpayOrderId);
        } catch (RazorpayIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Razorpay signature verification error: {}", e.getMessage(), e);
            throw new RazorpayIntegrationException("Razorpay signature verification error: " + e.getMessage(), e);
        }
    }

    private BigDecimal getPaidAmount(Long purchaseOrderId) {
        BigDecimal paid = paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(
                purchaseOrderId, PAID_STATUSES);
        return scale(paid == null ? ZERO : paid);
    }

    private PaymentStatus resolveOverallPaymentStatus(BigDecimal paidAmount, BigDecimal remainingAmount) {
        if (remainingAmount.compareTo(ZERO) <= 0) {
            return PaymentStatus.PAID;
        }
        if (paidAmount.compareTo(ZERO) > 0) {
            return PaymentStatus.PARTIALLY_PAID;
        }
        return PaymentStatus.INITIATED;
    }

    private BigDecimal resolveRequestedAmount(RazorpayInitiateRequest request, BigDecimal remainingAmount, boolean splitPaymentAllowed) {
        if (request.paymentAmount() == null) {
            return remainingAmount;
        }

        BigDecimal requestedAmount = scale(request.paymentAmount());
        if (requestedAmount.compareTo(ZERO) <= 0) {
            throw new InvalidPaymentRequestException("Payment amount must be greater than 0");
        }
        if (requestedAmount.compareTo(remainingAmount) > 0) {
            throw new InvalidPaymentRequestException("Payment amount cannot be greater than remaining amount");
        }
        if (requestedAmount.compareTo(remainingAmount) < 0 && !splitPaymentAllowed) {
            throw new AccessDeniedException("You are not allowed to split payments.");
        }

        return requestedAmount;
    }

    private void validateTransactionLimit(BigDecimal requestedAmount, BigDecimal remainingAmount, Long purchaseOrderId, String poNumber) {
        BigDecimal maxAllowedAmount = scale(razorpayMaxTransactionAmount);
        if (requestedAmount.compareTo(maxAllowedAmount) <= 0) {
            return;
        }

        publishLimitExceededAlert(purchaseOrderId, poNumber, remainingAmount);
        throw new PaymentLimitExceededException(
                "Payment amount exceeds Razorpay transaction limit. Please split the payment or contact admin.",
                requestedAmount,
                maxAllowedAmount,
                remainingAmount,
                true);
    }

    private List<BigDecimal> buildSuggestedSplits(BigDecimal requestedAmount) {
        BigDecimal maxAllowedAmount = scale(razorpayMaxTransactionAmount);
        java.util.ArrayList<BigDecimal> splits = new java.util.ArrayList<>();
        BigDecimal remaining = scale(requestedAmount);

        while (remaining.compareTo(maxAllowedAmount) > 0) {
            splits.add(maxAllowedAmount);
            remaining = scale(remaining.subtract(maxAllowedAmount));
        }

        if (remaining.compareTo(ZERO) > 0) {
            splits.add(remaining);
        }

        return List.copyOf(splits);
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String generatePaymentNumber() {
        String prefix = "PAY-" + LocalDate.now().format(NUMBER_DATE) + "-";
        int seq = 1;
        String candidate = prefix + String.format("%06d", seq);
        while (paymentRepository.existsByPaymentNumber(candidate)) {
            seq++;
            candidate = prefix + String.format("%06d", seq);
        }
        return candidate;
    }

    private void publishPaymentAlert(String routingKey, Payment payment, Long actorId, String message) {
        String correlationId = defaultCorrelationId(routingKey, payment.getPaymentId(), payment.getPurchaseOrderId());
        log.info("Publishing payment alert routingKey={} paymentId={} purchaseOrderId={} correlationId={}",
                routingKey, payment.getPaymentId(), payment.getPurchaseOrderId(), correlationId);
        paymentAlertPublisher.publish(routingKey, PaymentAlertEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(routingKey)
                .paymentId(payment.getPaymentId())
                .paymentNumber(payment.getPaymentNumber())
                .paymentReference(payment.getTransactionReference())
                .purchaseOrderId(payment.getPurchaseOrderId())
                .purchaseOrderNumber(payment.getPoNumber())
                .supplierId(payment.getSupplierId())
                .supplierName(payment.getSupplierName())
                .actorId(actorId)
                .totalAmount(payment.getPoTotalAmount())
                .paidAmount(payment.getPaymentAmount())
                .remainingAmount(payment.getRemainingAmount())
                .currency(payment.getCurrency())
                .message(message)
                .actionUrl(PAYMENTS_ACTION_URL)
                .sourceService(SOURCE_SERVICE)
                .correlationId(correlationId)
                .eventTime(LocalDateTime.now())
                .build());
    }

    private void publishLimitExceededAlert(Long purchaseOrderId, String poNumber, BigDecimal amount) {
        String correlationId = defaultCorrelationId(EVENT_LIMIT_EXCEEDED, purchaseOrderId, poNumber);
        log.warn("Publishing payment limit alert purchaseOrderId={} poNumber={} amount={} correlationId={}",
                purchaseOrderId, poNumber, amount, correlationId);
        paymentAlertPublisher.publish(EVENT_LIMIT_EXCEEDED, PaymentAlertEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(EVENT_LIMIT_EXCEEDED)
                .purchaseOrderId(purchaseOrderId)
                .purchaseOrderNumber(poNumber)
                .totalAmount(amount)
                .remainingAmount(amount)
                .currency("INR")
                .message("Payment amount exceeds Razorpay transaction limit. Please split the payment or contact admin.")
                .actionUrl(PAYMENTS_ACTION_URL)
                .sourceService(SOURCE_SERVICE)
                .correlationId(correlationId)
                .eventTime(LocalDateTime.now())
                .build());
        paymentAlertPublisher.publish(EVENT_SPLIT_RECOMMENDED, PaymentAlertEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(EVENT_SPLIT_RECOMMENDED)
                .purchaseOrderId(purchaseOrderId)
                .purchaseOrderNumber(poNumber)
                .totalAmount(amount)
                .remainingAmount(amount)
                .currency("INR")
                .message("Payment amount exceeds Razorpay transaction limit. Please split the payment or contact admin.")
                .actionUrl(PAYMENTS_ACTION_URL)
                .sourceService(SOURCE_SERVICE)
                .correlationId(correlationId + ":split")
                .eventTime(LocalDateTime.now())
                .build());
    }

    private String defaultCorrelationId(String eventType, Object primaryRef, Object secondaryRef) {
        return eventType + ":" + safe(primaryRef) + ":" + safe(secondaryRef);
    }

    private Payment findNonPaidPayment(String razorpayOrderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for Razorpay order ID: " + razorpayOrderId));
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new DuplicatePaymentException("Payment already verified for Razorpay order ID: " + razorpayOrderId);
        }
        return payment;
    }

    private Payment updatePaymentOutcome(Payment payment, PaymentStatus status, String razorpayPaymentId, Long actorId) {
        payment.setStatus(status);
        if (razorpayPaymentId != null && !razorpayPaymentId.isBlank()) {
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setTransactionReference(razorpayPaymentId);
        }
        if (actorId != null) {
            payment.setPaidBy(actorId);
        }
        payment.setPaymentDate(LocalDate.now());
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    private String resolveFailureReason(String requestReason, String fallback) {
        if (requestReason != null && !requestReason.isBlank()) {
            return requestReason.trim();
        }
        return fallback;
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
