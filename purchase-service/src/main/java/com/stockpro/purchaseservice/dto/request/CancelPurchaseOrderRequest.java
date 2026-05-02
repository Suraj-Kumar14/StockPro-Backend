package com.stockpro.purchaseservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelPurchaseOrderRequest(@NotBlank String cancellationReason) {
}
