package com.stockpro.supplierservice.dto.response;

import java.math.BigDecimal;

public record SupplierSummaryResponse(
        long totalSuppliers,
        long activeSuppliers,
        long inactiveSuppliers,
        long blacklistedSuppliers,
        long pendingReviewSuppliers,
        BigDecimal averageRating,
        BigDecimal averageLeadTimeDays
) {}
