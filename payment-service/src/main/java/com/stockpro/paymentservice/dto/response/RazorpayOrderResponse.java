package com.stockpro.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private String paymentNumber;
    private Long purchaseOrderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;
    private String description;
}
