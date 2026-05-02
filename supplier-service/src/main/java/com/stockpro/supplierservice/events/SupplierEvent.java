package com.stockpro.supplierservice.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SupplierEvent(
        String eventId,
        String eventType,
        Long supplierId,
        String supplierCode,
        String supplierName,
        String email,
        String phone,
        String paymentTerms,
        Integer leadTimeDays,
        BigDecimal rating,
        String status,
        Boolean isActive,
        Long actorId,
        LocalDateTime eventTime,
        String reason,
        Object oldValue,
        Object newValue
) {}
