package com.stockpro.supplierservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SupplierPerformanceResponse(
        Long supplierId,
        String supplierName,
        Integer totalOrders,
        Integer completedOrders,
        Integer delayedOrders,
        BigDecimal totalSpend,
        BigDecimal averageDeliveryDelayDays,
        BigDecimal qualityRating,
        BigDecimal deliveryRating,
        BigDecimal overallRating,
        LocalDateTime lastEvaluatedAt
) {}
