package com.stockpro.purchaseservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectPurchaseOrderRequest(@NotBlank String rejectionReason) {
}
