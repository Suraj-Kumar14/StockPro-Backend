package com.stockpro.purchaseservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentStatusSnapshotDTO {
    private Long paymentId;
    private String paymentNumber;
    private String paymentStatus;
    private boolean paymentCompleted;
    private BigDecimal paymentAmount;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime paidAt;
}
