package com.stockpro.paymentservice.service.impl;

import com.stockpro.paymentservice.client.PurchaseOrderLookupResponse;
import com.stockpro.paymentservice.client.PurchaseServiceClient;
import com.stockpro.paymentservice.client.SupplierLookupResponse;
import com.stockpro.paymentservice.client.SupplierServiceClient;
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
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.entity.PaymentHistory;
import com.stockpro.paymentservice.enums.PaymentAction;
import com.stockpro.paymentservice.enums.PaymentEventType;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.events.PaymentEvent;
import com.stockpro.paymentservice.exception.InvalidPaymentStateException;
import com.stockpro.paymentservice.exception.PaymentNotFoundException;
import com.stockpro.paymentservice.exception.PaymentValidationException;
import com.stockpro.paymentservice.mapper.PaymentMapper;
import com.stockpro.paymentservice.rabbitmq.PaymentEventPublisher;
import com.stockpro.paymentservice.repository.PaymentHistoryRepository;
import com.stockpro.paymentservice.repository.PaymentRepository;
import com.stockpro.paymentservice.service.PaymentService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<PaymentStatus> PAID_STATUSES = EnumSet.of(PaymentStatus.PAID, PaymentStatus.PARTIALLY_PAID);
    private static final Set<PaymentStatus> PENDING_PAYMENT_STATUSES = EnumSet.of(
            PaymentStatus.DRAFT,
            PaymentStatus.PENDING_APPROVAL,
            PaymentStatus.APPROVED);
    private static final Set<PaymentStatus> EXCLUDED_REMAINING_STATUSES = EnumSet.of(
            PaymentStatus.CANCELLED,
            PaymentStatus.REVERSED);
    private static final Set<String> ALLOWED_PO_STATUSES = Set.of("RECEIVED", "PARTIALLY_RECEIVED");
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PurchaseServiceClient purchaseServiceClient;
    private final SupplierServiceClient supplierServiceClient;
    private final PaymentMapper paymentMapper;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, Long actorId) {
        BigDecimal amount = scale(request.paymentAmount());
        validatePositiveAmount(amount);

        PurchaseOrderLookupResponse purchaseOrder = validatePurchaseOrder(request.purchaseOrderId());
        SupplierLookupResponse supplier = validateSupplier(request.supplierId() != null ? request.supplierId() : purchaseOrder.getSupplierId());

        BigDecimal previouslyPaidAmount = getPaidAmountForPurchaseOrder(request.purchaseOrderId());
        BigDecimal poTotalAmount = scale(purchaseOrder.getTotalAmount());
        assertAmountWithinRemaining(amount, poTotalAmount.subtract(previouslyPaidAmount));

        Payment payment = Payment.builder()
                .paymentNumber(generatePaymentNumber())
                .purchaseOrderId(request.purchaseOrderId())
                .poNumber(purchaseOrder.getPoNumber())
                .supplierId(supplier.getSupplierId())
                .supplierName(resolveSupplierName(supplier, purchaseOrder))
                .status(PaymentStatus.DRAFT)
                .paymentMethod(request.paymentMethod())
                .paymentAmount(amount)
                .poTotalAmount(poTotalAmount)
                .previouslyPaidAmount(previouslyPaidAmount)
                .remainingAmount(scale(poTotalAmount.subtract(previouslyPaidAmount).subtract(amount)))
                .currency("INR")
                .paymentDate(request.paymentDate())
                .transactionReference(trimToNull(request.transactionReference()))
                .bankReference(trimToNull(request.bankReference()))
                .remarks(trimToNull(request.remarks()))
                .createdBy(actorId)
                .build();

        payment.getHistory().add(history(payment, PaymentAction.CREATED, null, PaymentStatus.DRAFT, actorId, request.remarks()));
        Payment saved = paymentRepository.save(payment);
        log.info("Created payment paymentId={} paymentNumber={} purchaseOrderId={}", saved.getPaymentId(), saved.getPaymentNumber(), saved.getPurchaseOrderId());
        publish(saved, null, saved.getStatus(), PaymentEventType.PAYMENT_CREATED, "payment.created", actorId, null);
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse updatePayment(Long paymentId, UpdatePaymentRequest request, Long actorId) {
        Payment payment = getPaymentEntity(paymentId);
        ensureEditable(payment);

        BigDecimal amount = request.paymentAmount() != null ? scale(request.paymentAmount()) : payment.getPaymentAmount();
        validatePositiveAmount(amount);

        PurchaseOrderLookupResponse purchaseOrder = validatePurchaseOrder(payment.getPurchaseOrderId());
        SupplierLookupResponse supplier = validateSupplier(payment.getSupplierId());
        BigDecimal previouslyPaidAmount = getPaidAmountForPurchaseOrder(payment.getPurchaseOrderId());
        BigDecimal poTotalAmount = scale(purchaseOrder.getTotalAmount());
        assertAmountWithinRemaining(amount, poTotalAmount.subtract(previouslyPaidAmount));

        payment.setSupplierName(resolveSupplierName(supplier, purchaseOrder));
        payment.setPoNumber(purchaseOrder.getPoNumber());
        payment.setPoTotalAmount(poTotalAmount);
        payment.setPreviouslyPaidAmount(previouslyPaidAmount);
        payment.setPaymentAmount(amount);
        payment.setRemainingAmount(scale(poTotalAmount.subtract(previouslyPaidAmount).subtract(amount)));
        payment.setPaymentMethod(request.paymentMethod() != null ? request.paymentMethod() : payment.getPaymentMethod());
        payment.setPaymentDate(request.paymentDate() != null ? request.paymentDate() : payment.getPaymentDate());
        payment.setTransactionReference(trimToNull(request.transactionReference()));
        payment.setBankReference(trimToNull(request.bankReference()));
        payment.setRemarks(trimToNull(request.remarks()));
        payment.getHistory().add(history(payment, PaymentAction.UPDATED, payment.getStatus(), payment.getStatus(), actorId, request.remarks()));

        Payment saved = paymentRepository.save(payment);
        log.info("Updated payment paymentId={} status={}", saved.getPaymentId(), saved.getStatus());
        publish(saved, saved.getStatus(), saved.getStatus(), PaymentEventType.PAYMENT_UPDATED, "payment.updated", actorId, null);
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        return paymentMapper.toResponse(getPaymentEntity(paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByNumber(String paymentNumber) {
        return paymentMapper.toResponse(paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(int page, int size, String sortBy, String sortDir) {
        return paymentRepository.findAll(pageable(page, size, sortBy, sortDir)).map(paymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(PaymentSearchRequest request) {
        Pageable pageable = pageable(request.page(), request.size(), request.sortBy(), request.sortDir());
        return paymentRepository.findAll(specification(request), pageable).map(paymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByPurchaseOrder(Long purchaseOrderId, int page, int size) {
        return paymentRepository.findByPurchaseOrderId(purchaseOrderId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(paymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsBySupplier(Long supplierId, int page, int size) {
        return paymentRepository.findBySupplierId(supplierId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(paymentMapper::toResponse);
    }

    @Override
    @Transactional
    public PaymentResponse submitPayment(Long paymentId, SubmitPaymentRequest request, Long actorId) {
        Payment payment = getPaymentEntity(paymentId);
        PaymentStatus oldStatus = payment.getStatus();
        assertTransition(payment, PaymentStatus.DRAFT, "Only draft payments can be submitted");
        payment.setStatus(PaymentStatus.PENDING_APPROVAL);
        payment.setSubmittedBy(actorId);
        payment.setSubmittedAt(LocalDateTime.now());
        payment.getHistory().add(history(payment, PaymentAction.SUBMITTED, oldStatus, payment.getStatus(), actorId, request != null ? request.remarks() : null));

        Payment saved = paymentRepository.save(payment);
        log.info("Submitted payment paymentId={} paymentNumber={}", saved.getPaymentId(), saved.getPaymentNumber());
        publish(saved, oldStatus, saved.getStatus(), PaymentEventType.PAYMENT_SUBMITTED, "payment.submitted", actorId, request != null ? request.remarks() : null);
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse approvePayment(Long paymentId, ApprovePaymentRequest request, Long actorId) {
        Payment payment = getPaymentEntity(paymentId);
        PaymentStatus oldStatus = payment.getStatus();
        assertTransition(payment, PaymentStatus.PENDING_APPROVAL, "Only pending approval payments can be approved");
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setApprovedBy(actorId);
        payment.setApprovedAt(LocalDateTime.now());
        payment.getHistory().add(history(payment, PaymentAction.APPROVED, oldStatus, payment.getStatus(), actorId, request != null ? request.approvalRemarks() : null));

        Payment saved = paymentRepository.save(payment);
        log.info("Approved payment paymentId={} approvedBy={}", saved.getPaymentId(), actorId);
        publish(saved, oldStatus, saved.getStatus(), PaymentEventType.PAYMENT_APPROVED, "payment.approved", actorId, request != null ? request.approvalRemarks() : null);
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse rejectPayment(Long paymentId, RejectPaymentRequest request, Long actorId) {
        if (request == null || trimToNull(request.rejectionReason()) == null) {
            throw new PaymentValidationException("Rejection reason is required");
        }
        Payment payment = getPaymentEntity(paymentId);
        PaymentStatus oldStatus = payment.getStatus();
        assertTransition(payment, PaymentStatus.PENDING_APPROVAL, "Only pending approval payments can be rejected");
        payment.setStatus(PaymentStatus.REJECTED);
        payment.setRejectedBy(actorId);
        payment.setRejectedAt(LocalDateTime.now());
        payment.setRejectionReason(request.rejectionReason().trim());
        payment.getHistory().add(history(payment, PaymentAction.REJECTED, oldStatus, payment.getStatus(), actorId, request.rejectionReason()));

        Payment saved = paymentRepository.save(payment);
        log.info("Rejected payment paymentId={} rejectedBy={}", saved.getPaymentId(), actorId);
        publish(saved, oldStatus, saved.getStatus(), PaymentEventType.PAYMENT_REJECTED, "payment.rejected", actorId, request.rejectionReason());
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId, CancelPaymentRequest request, Long actorId) {
        if (request == null || trimToNull(request.cancellationReason()) == null) {
            throw new PaymentValidationException("Cancellation reason is required");
        }
        Payment payment = getPaymentEntity(paymentId);
        if (!EnumSet.of(PaymentStatus.DRAFT, PaymentStatus.PENDING_APPROVAL, PaymentStatus.APPROVED).contains(payment.getStatus())) {
            throw new InvalidPaymentStateException("Invalid payment status transition");
        }

        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledBy(actorId);
        payment.setCancelledAt(LocalDateTime.now());
        payment.setCancellationReason(request.cancellationReason().trim());
        payment.getHistory().add(history(payment, PaymentAction.CANCELLED, oldStatus, payment.getStatus(), actorId, request.cancellationReason()));

        Payment saved = paymentRepository.save(payment);
        log.info("Cancelled payment paymentId={} cancelledBy={}", saved.getPaymentId(), actorId);
        publish(saved, oldStatus, saved.getStatus(), PaymentEventType.PAYMENT_CANCELLED, "payment.cancelled", actorId, request.cancellationReason());
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse markPaymentPaid(Long paymentId, MarkPaymentPaidRequest request, Long actorId) {
        Payment payment = getPaymentEntity(paymentId);
        PaymentStatus oldStatus = payment.getStatus();
        assertTransition(payment, PaymentStatus.APPROVED, "Only approved payments can be marked as paid");

        PurchaseOrderLookupResponse purchaseOrder = validatePurchaseOrder(payment.getPurchaseOrderId());
        BigDecimal poTotalAmount = scale(purchaseOrder.getTotalAmount());
        BigDecimal previouslyPaidAmount = getPaidAmountForPurchaseOrder(payment.getPurchaseOrderId());
        assertAmountWithinRemaining(payment.getPaymentAmount(), poTotalAmount.subtract(previouslyPaidAmount));

        BigDecimal overallRemaining = scale(poTotalAmount.subtract(previouslyPaidAmount).subtract(payment.getPaymentAmount()));
        PaymentStatus newStatus = overallRemaining.compareTo(ZERO) == 0 ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID;

        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentDate(request.paymentDate());
        payment.setTransactionReference(trimToNull(request.transactionReference()));
        payment.setBankReference(trimToNull(request.bankReference()));
        payment.setRemarks(trimToNull(request.remarks()));
        payment.setPoTotalAmount(poTotalAmount);
        payment.setPreviouslyPaidAmount(previouslyPaidAmount);
        payment.setRemainingAmount(overallRemaining);
        payment.setStatus(newStatus);
        payment.setPaidBy(actorId);
        payment.setPaidAt(LocalDateTime.now());
        payment.getHistory().add(history(payment,
                newStatus == PaymentStatus.PAID ? PaymentAction.MARKED_PAID : PaymentAction.PARTIALLY_PAID,
                oldStatus,
                newStatus,
                actorId,
                request.remarks()));

        Payment saved = paymentRepository.save(payment);
        log.info("Marked payment paymentId={} as {}", saved.getPaymentId(), saved.getStatus());
        publish(saved,
                oldStatus,
                saved.getStatus(),
                saved.getStatus() == PaymentStatus.PAID ? PaymentEventType.PAYMENT_PAID : PaymentEventType.PAYMENT_PARTIALLY_PAID,
                saved.getStatus() == PaymentStatus.PAID ? "payment.paid" : "payment.partially-paid",
                actorId,
                request.remarks());
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse reversePayment(Long paymentId, ReversePaymentRequest request, Long actorId) {
        if (request == null || trimToNull(request.reversalReason()) == null) {
            throw new PaymentValidationException("Reversal reason is required");
        }
        Payment payment = getPaymentEntity(paymentId);
        if (!PAID_STATUSES.contains(payment.getStatus())) {
            throw new InvalidPaymentStateException("Only paid payments can be reversed");
        }

        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(PaymentStatus.REVERSED);
        payment.setReversedBy(actorId);
        payment.setReversedAt(LocalDateTime.now());
        payment.setReversalReason(request.reversalReason().trim());
        payment.getHistory().add(history(payment, PaymentAction.REVERSED, oldStatus, payment.getStatus(), actorId, request.reversalReason()));

        Payment saved = paymentRepository.save(payment);
        log.info("Reversed payment paymentId={} reversedBy={}", saved.getPaymentId(), actorId);
        publish(saved, oldStatus, saved.getStatus(), PaymentEventType.PAYMENT_REVERSED, "payment.reversed", actorId, request.reversalReason());
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory(Long paymentId) {
        getPaymentEntity(paymentId);
        return paymentHistoryRepository.findByPaymentPaymentIdOrderByActionAtAsc(paymentId).stream()
                .map(item -> PaymentHistoryResponse.builder()
                        .historyId(item.getHistoryId())
                        .paymentId(paymentId)
                        .action(item.getAction())
                        .oldStatus(item.getOldStatus())
                        .newStatus(item.getNewStatus())
                        .actorId(item.getActorId())
                        .remarks(item.getRemarks())
                        .actionAt(item.getActionAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary() {
        long totalPayments = paymentRepository.count();
        long draftCount = paymentRepository.countByStatus(PaymentStatus.DRAFT);
        long pendingApprovalCount = paymentRepository.countByStatus(PaymentStatus.PENDING_APPROVAL);
        long approvedCount = paymentRepository.countByStatus(PaymentStatus.APPROVED);
        long partiallyPaidCount = paymentRepository.countByStatus(PaymentStatus.PARTIALLY_PAID);
        long paidCount = paymentRepository.countByStatus(PaymentStatus.PAID);
        long cancelledCount = paymentRepository.countByStatus(PaymentStatus.CANCELLED);
        long rejectedCount = paymentRepository.countByStatus(PaymentStatus.REJECTED);
        long reversedCount = paymentRepository.countByStatus(PaymentStatus.REVERSED);
        BigDecimal totalPaidAmount = scale(paymentRepository.sumPaymentAmountByStatusIn(PAID_STATUSES));
        BigDecimal pendingAmount = scale(paymentRepository.sumPaymentAmountByStatusIn(PENDING_PAYMENT_STATUSES));
        BigDecimal remainingAmount = scale(paymentRepository.sumRemainingAmountExcludingStatuses(EXCLUDED_REMAINING_STATUSES));

        log.info("Payment summary generated totalPayments={} paidCount={} pendingApprovalCount={}",
                totalPayments, paidCount, pendingApprovalCount);

        return PaymentSummaryResponse.builder()
                .totalPayments(totalPayments)
                .draftCount(draftCount)
                .pendingApprovalCount(pendingApprovalCount)
                .approvedCount(approvedCount)
                .partiallyPaidCount(partiallyPaidCount)
                .paidCount(paidCount)
                .cancelledCount(cancelledCount)
                .rejectedCount(rejectedCount)
                .reversedCount(reversedCount)
                .totalPaidAmount(totalPaidAmount)
                .pendingPaymentAmount(pendingAmount)
                .remainingPaymentAmount(remainingAmount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentAnalyticsResponse getPaymentAnalytics(LocalDate fromDate, LocalDate toDate) {
        LocalDate effectiveFrom = fromDate != null ? fromDate : LocalDate.now().minusMonths(5).withDayOfMonth(1);
        LocalDate effectiveTo = toDate != null ? toDate : LocalDate.now();
        List<Payment> filtered = paymentRepository.findAll().stream()
                .filter(payment -> payment.getCreatedAt() != null)
                .filter(payment -> !payment.getCreatedAt().toLocalDate().isBefore(effectiveFrom))
                .filter(payment -> !payment.getCreatedAt().toLocalDate().isAfter(effectiveTo))
                .toList();

        Map<String, BigDecimal> monthlyTrend = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.from(effectiveFrom);
        YearMonth end = YearMonth.from(effectiveTo);
        while (!cursor.isAfter(end)) {
            monthlyTrend.put(cursor.toString(), ZERO);
            cursor = cursor.plusMonths(1);
        }
        filtered.stream()
                .filter(payment -> PAID_STATUSES.contains(payment.getStatus()))
                .forEach(payment -> {
                    String key = YearMonth.from(payment.getCreatedAt()).toString();
                    monthlyTrend.put(key, scale(monthlyTrend.getOrDefault(key, ZERO).add(payment.getPaymentAmount())));
                });

        Map<String, Long> byMethod = filtered.stream()
                .collect(LinkedHashMap::new,
                        (map, payment) -> map.merge(payment.getPaymentMethod().name(), 1L, Long::sum),
                        LinkedHashMap::putAll);

        Map<String, BigDecimal> bySupplier = new LinkedHashMap<>();
        filtered.stream()
                .filter(payment -> PAID_STATUSES.contains(payment.getStatus()))
                .forEach(payment -> bySupplier.merge(payment.getSupplierName(), payment.getPaymentAmount(), BigDecimal::add));
        bySupplier.replaceAll((key, value) -> scale(value));

        List<String> topSuppliers = bySupplier.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        return PaymentAnalyticsResponse.builder()
                .totalPaid(sumAmounts(filtered.stream()
                        .filter(payment -> PAID_STATUSES.contains(payment.getStatus()))
                        .map(Payment::getPaymentAmount)
                        .toList()))
                .totalPending(sumAmounts(filtered.stream()
                        .filter(payment -> EnumSet.of(PaymentStatus.DRAFT, PaymentStatus.PENDING_APPROVAL, PaymentStatus.APPROVED).contains(payment.getStatus()))
                        .map(Payment::getPaymentAmount)
                        .toList()))
                .monthlyPaidTrend(monthlyTrend)
                .paymentsByMethod(byMethod)
                .paymentsBySupplier(bySupplier)
                .pendingApprovals(filtered.stream().filter(payment -> payment.getStatus() == PaymentStatus.PENDING_APPROVAL).count())
                .topPaidSuppliers(topSuppliers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPaidAmountForPurchaseOrder(Long purchaseOrderId) {
        BigDecimal value = paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(purchaseOrderId, PAID_STATUSES);
        return scale(value == null ? ZERO : value);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingAmountForPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrderLookupResponse purchaseOrder = validatePurchaseOrder(purchaseOrderId);
        return scale(scale(purchaseOrder.getTotalAmount()).subtract(getPaidAmountForPurchaseOrder(purchaseOrderId)));
    }

    private Payment getPaymentEntity(Long paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
    }

    private void ensureEditable(Payment payment) {
        if (!EnumSet.of(PaymentStatus.DRAFT, PaymentStatus.PENDING_APPROVAL).contains(payment.getStatus())) {
            if (PAID_STATUSES.contains(payment.getStatus())) {
                throw new InvalidPaymentStateException("Paid payment cannot be updated");
            }
            if (payment.getStatus() == PaymentStatus.REVERSED) {
                throw new InvalidPaymentStateException("Reversed payment cannot be changed");
            }
            throw new InvalidPaymentStateException("Invalid payment status transition");
        }
    }

    private void assertTransition(Payment payment, PaymentStatus expected, String message) {
        if (payment.getStatus() != expected) {
            log.warn("Invalid payment lifecycle transition paymentId={} currentStatus={} expected={}",
                    payment.getPaymentId(), payment.getStatus(), expected);
            throw new InvalidPaymentStateException(message);
        }
    }

    private PurchaseOrderLookupResponse validatePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrderLookupResponse purchaseOrder = purchaseServiceClient.getPurchaseOrder(purchaseOrderId);
        if (purchaseOrder.getStatus() == null || !ALLOWED_PO_STATUSES.contains(purchaseOrder.getStatus().toUpperCase(Locale.ROOT))) {
            log.warn("Payment creation rejected for purchaseOrderId={} status={}", purchaseOrderId, purchaseOrder.getStatus());
            throw new PaymentValidationException("Cannot create payment for this PO status");
        }
        if (purchaseOrder.getTotalAmount() == null || purchaseOrder.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Purchase order total amount is invalid");
        }
        return purchaseOrder;
    }

    private SupplierLookupResponse validateSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new PaymentValidationException("Supplier not found or inactive");
        }
        return supplierServiceClient.getSupplier(supplierId);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new PaymentValidationException("Payment amount must be greater than zero");
        }
    }

    private void assertAmountWithinRemaining(BigDecimal amount, BigDecimal remaining) {
        BigDecimal safeRemaining = scale(remaining);
        if (amount.compareTo(safeRemaining) > 0) {
            log.warn("Overpayment attempt amount={} remaining={}", amount, safeRemaining);
            throw new PaymentValidationException("Payment amount cannot exceed remaining PO amount");
        }
    }

    private String generatePaymentNumber() {
        String prefix = "PAY-" + LocalDate.now().format(NUMBER_DATE) + "-";
        int sequence = 1;
        String candidate = prefix + String.format("%06d", sequence);
        while (paymentRepository.existsByPaymentNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%06d", sequence);
        }
        return candidate;
    }

    private PaymentHistory history(Payment payment, PaymentAction action, PaymentStatus oldStatus, PaymentStatus newStatus, Long actorId, String remarks) {
        return PaymentHistory.builder()
                .payment(payment)
                .action(action.name())
                .oldStatus(oldStatus != null ? oldStatus.name() : null)
                .newStatus(newStatus != null ? newStatus.name() : null)
                .actorId(actorId)
                .remarks(trimToNull(remarks))
                .build();
    }

    private void publish(Payment payment, PaymentStatus oldStatus, PaymentStatus newStatus, PaymentEventType eventType,
                         String routingKey, Long actorId, String reason) {
        paymentEventPublisher.publish(routingKey, PaymentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .paymentId(payment.getPaymentId())
                .paymentNumber(payment.getPaymentNumber())
                .purchaseOrderId(payment.getPurchaseOrderId())
                .poNumber(payment.getPoNumber())
                .supplierId(payment.getSupplierId())
                .supplierName(payment.getSupplierName())
                .status(payment.getStatus())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .paymentAmount(payment.getPaymentAmount())
                .remainingAmount(payment.getRemainingAmount())
                .paymentMethod(payment.getPaymentMethod())
                .actorId(actorId)
                .eventTime(LocalDateTime.now())
                .reason(trimToNull(reason))
                .build());
    }

    private Specification<Payment> specification(PaymentSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (trimToNull(request.keyword()) != null) {
                String like = "%" + request.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("paymentNumber")), like),
                        cb.like(cb.lower(root.get("poNumber")), like),
                        cb.like(cb.lower(root.get("supplierName")), like),
                        cb.like(cb.lower(root.get("transactionReference")), like),
                        cb.like(cb.lower(root.get("bankReference")), like)
                ));
            }
            if (request.purchaseOrderId() != null) {
                predicates.add(cb.equal(root.get("purchaseOrderId"), request.purchaseOrderId()));
            }
            if (request.supplierId() != null) {
                predicates.add(cb.equal(root.get("supplierId"), request.supplierId()));
            }
            if (request.status() != null) {
                predicates.add(cb.equal(root.get("status"), request.status()));
            }
            if (request.paymentMethod() != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), request.paymentMethod()));
            }
            if (request.createdBy() != null) {
                predicates.add(cb.equal(root.get("createdBy"), request.createdBy()));
            }
            if (request.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), request.fromDate()));
            }
            if (request.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), request.toDate()));
            }
            if (request.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paymentAmount"), scale(request.minAmount())));
            }
            if (request.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paymentAmount"), scale(request.maxAmount())));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable pageable(int page, int size, String sortBy, String sortDir) {
        String field = trimToNull(sortBy) != null ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), size > 0 ? size : 10, Sort.by(direction, field));
    }

    private String resolveSupplierName(SupplierLookupResponse supplier, PurchaseOrderLookupResponse purchaseOrder) {
        if (trimToNull(supplier.getName()) != null) {
            return supplier.getName().trim();
        }
        return trimToNull(purchaseOrder.getSupplierName());
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumAmounts(List<BigDecimal> amounts) {
        return amounts.stream().filter(value -> value != null).reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
