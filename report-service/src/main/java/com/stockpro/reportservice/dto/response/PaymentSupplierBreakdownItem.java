package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record PaymentSupplierBreakdownItem(
        Long supplierId,
        String supplierName,
        BigDecimal totalPaid,
        long paymentCount) {
}
