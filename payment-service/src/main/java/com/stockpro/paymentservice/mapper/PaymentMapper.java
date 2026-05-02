package com.stockpro.paymentservice.mapper;

import com.stockpro.paymentservice.dto.response.PaymentHistoryResponse;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.entity.PaymentHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentNumber(payment.getPaymentNumber())
                .purchaseOrderId(payment.getPurchaseOrderId())
                .poNumber(payment.getPoNumber())
                .supplierId(payment.getSupplierId())
                .supplierName(payment.getSupplierName())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .paymentAmount(payment.getPaymentAmount())
                .poTotalAmount(payment.getPoTotalAmount())
                .previouslyPaidAmount(payment.getPreviouslyPaidAmount())
                .remainingAmount(payment.getRemainingAmount())
                .currency(payment.getCurrency())
                .paymentDate(payment.getPaymentDate())
                .transactionReference(payment.getTransactionReference())
                .bankReference(payment.getBankReference())
                .remarks(payment.getRemarks())
                .rejectionReason(payment.getRejectionReason())
                .cancellationReason(payment.getCancellationReason())
                .reversalReason(payment.getReversalReason())
                .createdBy(payment.getCreatedBy())
                .approvedBy(payment.getApprovedBy())
                .paidBy(payment.getPaidBy())
                .submittedAt(payment.getSubmittedAt())
                .approvedAt(payment.getApprovedAt())
                .rejectedAt(payment.getRejectedAt())
                .cancelledAt(payment.getCancelledAt())
                .paidAt(payment.getPaidAt())
                .reversedAt(payment.getReversedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .history(toHistory(payment.getHistory(), payment.getPaymentId()))
                .build();
    }

    public List<PaymentHistoryResponse> toHistory(List<PaymentHistory> history, Long paymentId) {
        return history.stream().map(item -> PaymentHistoryResponse.builder()
                .historyId(item.getHistoryId())
                .paymentId(paymentId)
                .action(item.getAction())
                .oldStatus(item.getOldStatus())
                .newStatus(item.getNewStatus())
                .actorId(item.getActorId())
                .remarks(item.getRemarks())
                .actionAt(item.getActionAt())
                .build()).toList();
    }
}
