package com.stockpro.paymentservice.dto.request;

import com.stockpro.paymentservice.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePaymentRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal paymentAmount,
        @NotNull PaymentMethod paymentMethod,
        LocalDate paymentDate,
        String transactionReference,
        String bankReference,
        String remarks) {
}
