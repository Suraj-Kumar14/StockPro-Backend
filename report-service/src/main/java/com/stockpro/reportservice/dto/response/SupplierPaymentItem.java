package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record SupplierPaymentItem(
        Long supplierId,
        String supplierName,
        BigDecimal paidAmount,
        BigDecimal pendingAmount) {
}
