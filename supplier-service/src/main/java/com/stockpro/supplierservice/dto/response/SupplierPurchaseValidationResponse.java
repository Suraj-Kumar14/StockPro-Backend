package com.stockpro.supplierservice.dto.response;

import com.stockpro.supplierservice.entity.SupplierStatus;

public record SupplierPurchaseValidationResponse(
        Long supplierId,
        String supplierName,
        Boolean isActive,
        SupplierStatus status,
        String paymentTerms,
        Integer leadTimeDays,
        Boolean canUseForPurchase,
        String reason
) {}
