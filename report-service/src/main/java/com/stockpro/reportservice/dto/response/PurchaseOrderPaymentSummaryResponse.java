package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseOrderPaymentSummaryResponse(
        BigDecimal totalPaid,
        BigDecimal remainingAmount,
        List<PaymentLineItemResponse> payments) {
}
