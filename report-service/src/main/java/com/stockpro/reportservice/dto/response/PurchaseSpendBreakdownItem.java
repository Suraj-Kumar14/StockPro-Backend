package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record PurchaseSpendBreakdownItem(
        Long id,
        String name,
        long poCount,
        BigDecimal totalSpend) {
}
