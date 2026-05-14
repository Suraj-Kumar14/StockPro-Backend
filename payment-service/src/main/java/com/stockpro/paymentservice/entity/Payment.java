package com.stockpro.paymentservice.entity;

import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.persistence.PaymentMethodConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false, unique = true, length = 32)
    private String paymentNumber;

    @Column(nullable = false)
    private Long purchaseOrderId;

    @Column(length = 50)
    private String poNumber;

    @Column(nullable = false)
    private Long supplierId;

    @Column(length = 255)
    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Convert(converter = PaymentMethodConverter.class)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal poTotalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal previouslyPaidAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    private LocalDate paymentDate;

    @Column(length = 100)
    private String transactionReference;

    // Razorpay-specific fields
    @Column(length = 100, unique = true)
    private String razorpayOrderId;

    @Column(length = 100)
    private String razorpayPaymentId;

    @Column(length = 500)
    private String razorpaySignature;

    private Long createdBy;
    private Long paidBy;

    private LocalDateTime paidAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = PaymentStatus.INITIATED;
        if (currency == null || currency.isBlank()) currency = "INR";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
