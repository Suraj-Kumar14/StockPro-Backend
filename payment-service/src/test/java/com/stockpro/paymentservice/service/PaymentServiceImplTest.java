package com.stockpro.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.paymentservice.dto.response.PaymentSummaryResponse;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.exception.PaymentNotFoundException;
import com.stockpro.paymentservice.mapper.PaymentMapper;
import com.stockpro.paymentservice.repository.PaymentRepository;
import com.stockpro.paymentservice.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, new PaymentMapper());
    }

    @Test
    void getPaymentById_shouldReturnMappedPayment() {
        when(paymentRepository.findByPaymentId(1L)).thenReturn(Optional.of(payment("PAY-001", PaymentStatus.APPROVED, new BigDecimal("400.00"))));

        var response = paymentService.getPaymentById(1L);

        assertEquals("PAY-001", response.paymentNumber());
        assertEquals(PaymentStatus.APPROVED, response.status());
    }

    @Test
    void getPaymentById_shouldThrowWhenMissing() {
        when(paymentRepository.findByPaymentId(99L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(99L));
    }

    @Test
    void getAllPayments_shouldSanitizePagingAndFallbackSortField() {
        when(paymentRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment("PAY-001", PaymentStatus.APPROVED, new BigDecimal("400.00")))));

        var page = paymentService.getAllPayments(-1, 0, "unsupported", "asc");

        verify(paymentRepository).findAll(any(Pageable.class));
        assertEquals(1, page.getTotalElements());
        assertEquals("PAY-001", page.getContent().get(0).paymentNumber());
    }

    @Test
    void searchPayments_shouldApplySpecificationAndMapResults() {
        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment("PAY-002", PaymentStatus.PAID, new BigDecimal("250.00")))));

        var page = paymentService.searchPayments(7L, PaymentStatus.PAID, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 9), 0, 10, "paymentDate", "desc");

        assertEquals(1, page.getTotalElements());
        assertEquals(PaymentStatus.PAID, page.getContent().get(0).status());
    }

    @Test
    void getPaymentSummary_shouldAggregateFromExistingPayments() {
        when(paymentRepository.findAll()).thenReturn(List.of(
                payment("PAY-001", PaymentStatus.PAID, new BigDecimal("400.00"), new BigDecimal("0.00")),
                payment("PAY-002", PaymentStatus.PARTIALLY_PAID, new BigDecimal("200.00"), new BigDecimal("300.00")),
                payment("PAY-003", PaymentStatus.PENDING_APPROVAL, new BigDecimal("100.00"), new BigDecimal("400.00"))));

        PaymentSummaryResponse response = paymentService.getPaymentSummary();

        assertEquals(3, response.totalPayments());
        assertEquals(1, response.pendingApprovalCount());
        assertEquals(1, response.partiallyPaidCount());
        assertEquals(new BigDecimal("600.00"), response.totalPaidAmount());
        assertEquals(new BigDecimal("300.00"), response.pendingPaymentAmount());
        assertEquals(new BigDecimal("700.00"), response.remainingPaymentAmount());
    }

    @Test
    void getPaymentsByPurchaseOrderAndPaidAmount_shouldUseRepositoryQueries() {
        when(paymentRepository.findByPurchaseOrderId(org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(payment("PAY-001", PaymentStatus.PAID, new BigDecimal("400.00")))));
        when(paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.<Set<PaymentStatus>>any()))
                .thenReturn(new BigDecimal("120.5"));

        var page = paymentService.getPaymentsByPurchaseOrder(101L, 0, 10);

        assertEquals(1, page.getTotalElements());
        assertEquals(new BigDecimal("120.50"), paymentService.getPaidAmountForPurchaseOrder(101L));
    }

    private Payment payment(String paymentNumber, PaymentStatus status, BigDecimal amount) {
        return payment(paymentNumber, status, amount, new BigDecimal("500.00"));
    }

    private Payment payment(String paymentNumber, PaymentStatus status, BigDecimal amount, BigDecimal remainingAmount) {
        return Payment.builder()
                .paymentId(1L)
                .paymentNumber(paymentNumber)
                .purchaseOrderId(101L)
                .poNumber("PO-101")
                .supplierId(7L)
                .supplierName("Acme Supplies")
                .status(status)
                .paymentMethod(PaymentMethod.RAZORPAY)
                .paymentAmount(amount)
                .poTotalAmount(new BigDecimal("1000.00"))
                .previouslyPaidAmount(new BigDecimal("100.00"))
                .remainingAmount(remainingAmount)
                .currency("INR")
                .paymentDate(LocalDate.of(2026, 5, 9))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
