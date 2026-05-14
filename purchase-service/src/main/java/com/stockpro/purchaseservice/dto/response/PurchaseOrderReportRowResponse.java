package com.stockpro.purchaseservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseOrderReportRowResponse(
        Long purchaseOrderId,
        String poNumber,
        String purchaseOrderStatus,
        String paymentStatus,
        String paymentNumber,
        String razorpayOrderId,
        String razorpayPaymentId,
        BigDecimal paymentAmount,
        LocalDateTime paidAt,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String productSku,
        String productName,
        String productCategory,
        BigDecimal unitPrice,
        Integer orderedQuantity,
        Integer receivedQuantity,
        Integer remainingQuantity,
        BigDecimal lineTotal,
        BigDecimal purchaseOrderTotalAmount,
        LocalDate orderDate,
        LocalDate expectedDate,
        Long approvedBy,
        LocalDateTime approvedAt,
        LocalDateTime createdAt) {
}
