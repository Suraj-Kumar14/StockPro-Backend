package com.stockpro.supplierservice.dto.response;

import com.stockpro.supplierservice.entity.SupplierStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SupplierResponse(
        Long supplierId,
        String supplierCode,
        String name,
        String contactPerson,
        String email,
        String phone,
        String alternatePhone,
        String address,
        String city,
        String state,
        String country,
        String postalCode,
        String taxNumber,
        String gstNumber,
        String paymentTerms,
        Integer leadTimeDays,
        BigDecimal rating,
        SupplierStatus status,
        Boolean isActive,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long createdBy,
        Long updatedBy
) {}
