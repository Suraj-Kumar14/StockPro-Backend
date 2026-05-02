package com.stockpro.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectPaymentRequest(@NotBlank String rejectionReason) {
}
