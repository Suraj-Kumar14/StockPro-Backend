package com.stockpro.purchaseservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long purchaseOrderId,
        String poNumber,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        Long createdBy,
        String createdByName,
        Long approvedBy,
        String approvedByName,
        String status,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        LocalDate expectedDeliveryDate,
        LocalDate actualDeliveryDate,
        String paymentTerms,
        String notes,
        String approvalRemarks,
        String rejectionReason,
        String cancellationReason,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        LocalDateTime cancelledAt,
        LocalDateTime receivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isOverdue,
        List<PurchaseOrderLineItemResponse> lineItems,
        List<PurchaseOrderHistoryResponse> history) {
}
