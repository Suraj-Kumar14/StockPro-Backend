package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderDetailReportResponse(
        Long purchaseOrderId,
        String poNumber,
        String supplierName,
        String warehouseName,
        String status,
        Long createdBy,
        String createdByName,
        Long approvedBy,
        String approvedByName,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDate expectedDeliveryDate,
        boolean isOverdue,
        BigDecimal totalAmount,
        String paymentStatus,
        List<PurchaseOrderDetailItemResponse> items,
        List<PurchaseOrderTimelineItemResponse> timeline,
        PurchaseOrderPaymentSummaryResponse paymentSummary,
        String cancellationReason,
        String rejectionReason,
        LocalDateTime receivedAt) {
}
