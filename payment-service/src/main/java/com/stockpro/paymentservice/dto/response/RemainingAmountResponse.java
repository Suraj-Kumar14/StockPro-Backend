package com.stockpro.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.stockpro.paymentservice.enums.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemainingAmountResponse {
    private Long purchaseOrderId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private PaymentStatus status;
    private BigDecimal maxAllowedAmount;
    private String currency;
}
