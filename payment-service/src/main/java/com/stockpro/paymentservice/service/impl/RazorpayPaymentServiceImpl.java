package com.stockpro.paymentservice.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.stockpro.paymentservice.client.PurchaseOrderLookupResponse;
import com.stockpro.paymentservice.client.PurchaseServiceClient;
import com.stockpro.paymentservice.dto.request.RazorpayInitiateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayVerifyRequest;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.RazorpayOrderResponse;
import com.stockpro.paymentservice.dto.response.RemainingAmountResponse;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.exception.DuplicatePaymentException;
import com.stockpro.paymentservice.exception.PaymentNotFoundException;
import com.stockpro.paymentservice.exception.PaymentValidationException;
import com.stockpro.paymentservice.exception.RazorpayIntegrationException;
import com.stockpro.paymentservice.mapper.PaymentMapper;
import com.stockpro.paymentservice.repository.PaymentRepository;
import com.stockpro.paymentservice.service.RazorpayPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class RazorpayPaymentServiceImpl implements RazorpayPaymentService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<PaymentStatus> PAID_STATUSES = EnumSet.of(PaymentStatus.PAID, PaymentStatus.PARTIALLY_PAID);
    private static final Set<String> ALLOWED_PO_STATUSES = Set.of("APPROVED", "RECEIVED", "PARTIALLY_RECEIVED");
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final PaymentRepository paymentRepository;
    private final PurchaseServiceClient purchaseServiceClient;
    private final PaymentMapper paymentMapper;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    public RazorpayPaymentServiceImpl(PaymentRepository paymentRepository,
                                      PurchaseServiceClient purchaseServiceClient,
                                      PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.purchaseServiceClient = purchaseServiceClient;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public RazorpayOrderResponse initiatePayment(RazorpayInitiateRequest request, Long actorId, String authToken) {
        log.info("Initiating Razorpay payment for purchaseOrderId={} actorId={}", request.purchaseOrderId(), actorId);

        // Defense-in-depth null guard (should be caught by @Valid, but guards against edge cases)
        if (request.purchaseOrderId() == null) {
            throw new PaymentValidationException("Purchase order ID must not be null");
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

        // 4. Create Razorpay order (amount in paise)
        long amountInPaise = remaining.multiply(BigDecimal.valueOf(100)).longValue();
        String razorpayOrderId = createRazorpayOrder(amountInPaise, request.purchaseOrderId());

        // 5. Persist payment record (PENDING_APPROVAL = waiting for Razorpay verification)
        String paymentNumber = generatePaymentNumber();
        Long supplierId = po.getSupplierId() != null ? po.getSupplierId() : 0L;
        String supplierName = po.getSupplierName();

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .purchaseOrderId(request.purchaseOrderId())
                .poNumber(po.getPoNumber())
                .supplierId(supplierId)
                .supplierName(supplierName)
                .status(PaymentStatus.PENDING_APPROVAL)
                .paymentMethod(PaymentMethod.RAZORPAY)
                .paymentAmount(remaining)
                .poTotalAmount(poTotal)
                .previouslyPaidAmount(paidSoFar)
                .remainingAmount(ZERO)
                .currency("INR")
                .razorpayOrderId(razorpayOrderId)
                .createdBy(actorId)
                .build();

        paymentRepository.save(payment);
        log.info("Created Razorpay payment record paymentNumber={} razorpayOrderId={}", paymentNumber, razorpayOrderId);

        return RazorpayOrderResponse.builder()
                .razorpayOrderId(razorpayOrderId)
                .paymentNumber(paymentNumber)
                .purchaseOrderId(request.purchaseOrderId())
                .amount(remaining)
                .currency("INR")
                .keyId(razorpayKeyId)
                .description("Payment for PO " + (po.getPoNumber() != null ? po.getPoNumber() : request.purchaseOrderId()))
                .build();
    }


    @Override
    @Transactional
    public PaymentResponse verifyPayment(RazorpayVerifyRequest request, Long actorId) {
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
        verifyRazorpaySignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());

        // 4. Mark payment as PAID
        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setTransactionReference(request.razorpayPaymentId());
        payment.setPaidBy(actorId);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaymentDate(LocalDate.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Payment verified and marked PAID paymentId={} paymentNumber={} razorpayPaymentId={}",
                saved.getPaymentId(), saved.getPaymentNumber(), request.razorpayPaymentId());

        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RemainingAmountResponse getRemainingAmount(Long purchaseOrderId, String authToken) {
        PurchaseOrderLookupResponse po = purchaseServiceClient.getPurchaseOrder(purchaseOrderId, authToken);
        BigDecimal total = scale(po.getTotalAmount());
        BigDecimal paid = getPaidAmount(purchaseOrderId);
        BigDecimal remaining = scale(total.subtract(paid));

        return RemainingAmountResponse.builder()
                .purchaseOrderId(purchaseOrderId)
                .totalAmount(total)
                .paidAmount(paid)
                .remainingAmount(remaining.compareTo(ZERO) < 0 ? ZERO : remaining)
                .currency("INR")
                .build();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void validatePoStatus(PurchaseOrderLookupResponse po) {
        if (po.getStatus() == null || !ALLOWED_PO_STATUSES.contains(po.getStatus().toUpperCase(Locale.ROOT))) {
            log.warn("Razorpay payment rejected: PO purchaseOrderId={} has status={}",
                    po.getPurchaseOrderId() != null ? po.getPurchaseOrderId() : po.getPoId(), po.getStatus());
            throw new PaymentValidationException(
                    "Cannot initiate payment for a PO with status: " + po.getStatus()
                    + ". Only APPROVED purchase orders can be paid.");
        }
        if (po.getTotalAmount() == null || po.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Purchase order total amount is invalid or zero.");
        }
    }

    private String createRazorpayOrder(long amountInPaise, Long purchaseOrderId) {
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
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            throw new RazorpayIntegrationException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
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
}
