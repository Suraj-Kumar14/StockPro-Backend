package com.stockpro.paymentservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment approvedPayment;
    private Payment paidPayment;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        approvedPayment = paymentRepository.save(payment(101L, "PAY-001", PaymentStatus.INITIATED, new BigDecimal("400.00"), "order_1"));
        paidPayment = paymentRepository.save(payment(101L, "PAY-002", PaymentStatus.PAID, new BigDecimal("250.00"), "order_2"));
        paymentRepository.save(payment(102L, "PAY-003", PaymentStatus.DRAFT, new BigDecimal("125.00"), null));
    }

    @Test
    void lookupMethods_shouldFindByIdsAndOrderNumbers() {
        assertTrue(paymentRepository.findByPaymentId(approvedPayment.getPaymentId()).isPresent());
        assertTrue(paymentRepository.findByRazorpayOrderId("order_2").isPresent());
        assertTrue(paymentRepository.existsByPaymentNumber("PAY-001"));
        assertFalse(paymentRepository.existsByPaymentNumber("PAY-999"));
    }

    @Test
    void purchaseOrderQueries_shouldReturnPagedResultsAndAggregates() {
        var page = paymentRepository.findByPurchaseOrderId(101L, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(paymentRepository.existsByPurchaseOrderIdAndStatusIn(101L, List.of(PaymentStatus.INITIATED, PaymentStatus.PAID)));
        assertEquals(new BigDecimal("650.00"),
                paymentRepository.sumPaymentAmountByPurchaseOrderIdAndStatusIn(101L, List.of(PaymentStatus.INITIATED, PaymentStatus.PAID)));
    }

    private Payment payment(Long purchaseOrderId, String paymentNumber, PaymentStatus status, BigDecimal amount, String razorpayOrderId) {
        return Payment.builder()
                .paymentNumber(paymentNumber)
                .purchaseOrderId(purchaseOrderId)
                .poNumber("PO-" + purchaseOrderId)
                .supplierId(7L)
                .supplierName("Acme Supplies")
                .status(status)
                .paymentMethod(PaymentMethod.RAZORPAY)
                .paymentAmount(amount)
                .poTotalAmount(new BigDecimal("1000.00"))
                .previouslyPaidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("1000.00").subtract(amount))
                .currency("INR")
                .paymentDate(LocalDate.of(2026, 5, 9))
                .transactionReference("TXN-" + paymentNumber)
                .razorpayOrderId(razorpayOrderId)
                .createdBy(1L)
                .build();
    }
}
