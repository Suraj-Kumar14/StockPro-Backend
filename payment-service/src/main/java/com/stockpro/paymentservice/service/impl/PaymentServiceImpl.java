package com.stockpro.paymentservice.service.impl;

import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.exception.PaymentNotFoundException;
import com.stockpro.paymentservice.mapper.PaymentMapper;
import com.stockpro.paymentservice.repository.PaymentRepository;
import com.stockpro.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 10, Sort.by(dir, field));
        return paymentRepository.findAll(pageable).map(paymentMapper::toResponse);
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
}
