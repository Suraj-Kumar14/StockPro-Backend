package com.stockpro.purchaseservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotNull Long supplierId,
        @NotNull Long warehouseId,
        @NotNull LocalDate expectedDeliveryDate,
        String paymentTerms,
        String notes,
        @Valid @NotEmpty List<CreatePurchaseOrderLineItemRequest> lineItems) {
}
