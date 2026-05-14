package com.stockpro.paymentservice.mapper;

import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.entity.Payment;
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
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .createdBy(payment.getCreatedBy())
                .paidBy(payment.getPaidBy())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
