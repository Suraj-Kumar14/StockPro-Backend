package com.stockpro.purchaseservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePurchaseOrderLineItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer orderedQuantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitCost,
        String notes) {
}
