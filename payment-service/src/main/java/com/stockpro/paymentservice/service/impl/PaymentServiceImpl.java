package com.stockpro.paymentservice.service.impl;

import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.PaymentSummaryResponse;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.exception.PaymentNotFoundException;
import com.stockpro.paymentservice.mapper.PaymentMapper;
import com.stockpro.paymentservice.repository.PaymentRepository;
import com.stockpro.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("paymentId", "paymentNumber", "purchaseOrderId", "supplierId", "status", "paymentAmount", "paymentDate", "createdAt", "updatedAt");
    private static final Set<PaymentStatus> PAID_STATUSES =
            EnumSet.of(PaymentStatus.PAID, PaymentStatus.PARTIALLY_PAID);

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        return paymentMapper.toResponse(
                paymentRepository.findByPaymentId(paymentId)
                        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        log.info("Fetching payments page={} size={} sortBy={} sortDir={}", page, size, sortBy, sortDir);
        return paymentRepository.findAll(pageable).map(paymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(Long supplierId, PaymentStatus status, LocalDate fromDate, LocalDate toDate,
                                                int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Specification<Payment> specification = Specification.where(null);

        if (supplierId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("supplierId"), supplierId));
        }
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (fromDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
        }

        return paymentRepository.findAll(specification, pageable).map(paymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary() {
        List<Payment> payments = paymentRepository.findAll();
        BigDecimal totalPaidAmount = payments.stream()
                .filter(payment -> PAID_STATUSES.contains(payment.getStatus()))
                .map(Payment::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal pendingPaymentAmount = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING_APPROVAL
                        || payment.getStatus() == PaymentStatus.APPROVED
                        || payment.getStatus() == PaymentStatus.PARTIALLY_PAID)
                .map(Payment::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal remainingPaymentAmount = payments.stream()
                .filter(payment -> payment.getStatus() != PaymentStatus.PAID
                        && payment.getStatus() != PaymentStatus.CANCELLED
                        && payment.getStatus() != PaymentStatus.REJECTED
                        && payment.getStatus() != PaymentStatus.REVERSED)
                .map(Payment::getRemainingAmount)
                .filter(amount -> amount != null)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new PaymentSummaryResponse(
                payments.size(),
                countByStatus(payments, PaymentStatus.DRAFT),
                countByStatus(payments, PaymentStatus.PENDING_APPROVAL),
                countByStatus(payments, PaymentStatus.APPROVED),
                countByStatus(payments, PaymentStatus.PARTIALLY_PAID),
                countByStatus(payments, PaymentStatus.PAID),
                countByStatus(payments, PaymentStatus.CANCELLED),
                countByStatus(payments, PaymentStatus.REJECTED),
                countByStatus(payments, PaymentStatus.REVERSED),
                totalPaidAmount,
                pendingPaymentAmount,
                remainingPaymentAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByPurchaseOrder(Long purchaseOrderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return paymentRepository.findByPurchaseOrderId(purchaseOrderId, pageable)
                .map(paymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPaidAmountForPurchaseOrder(Long purchaseOrderId) {
        BigDecimal value = paymentRepository
                .sumPaymentAmountByPurchaseOrderIdAndStatusIn(purchaseOrderId, PAID_STATUSES);
        return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String requestedField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        String field = ALLOWED_SORT_FIELDS.contains(requestedField) ? requestedField : "createdAt";
        return PageRequest.of(Math.max(page, 0), size > 0 ? size : 10, Sort.by(dir, field));
    }

    private long countByStatus(List<Payment> payments, PaymentStatus status) {
        return payments.stream().filter(payment -> payment.getStatus() == status).count();
    }
}
