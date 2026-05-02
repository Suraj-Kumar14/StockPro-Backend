package com.stockpro.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReversePaymentRequest(@NotBlank String reversalReason) {
}
