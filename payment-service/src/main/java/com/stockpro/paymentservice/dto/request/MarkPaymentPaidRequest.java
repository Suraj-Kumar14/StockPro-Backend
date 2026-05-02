package com.stockpro.paymentservice.dto.request;

import com.stockpro.paymentservice.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MarkPaymentPaidRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull LocalDate paymentDate,
        String transactionReference,
        String bankReference,
        String remarks) {
}
