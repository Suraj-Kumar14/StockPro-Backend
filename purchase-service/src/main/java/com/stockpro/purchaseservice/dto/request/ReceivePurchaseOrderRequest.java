package com.stockpro.purchaseservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

public record ReceivePurchaseOrderRequest(
        String receiptReference,
        LocalDate receivedDate,
        String notes,
        @Valid @NotEmpty List<ReceivePurchaseOrderLineItemRequest> lineItems) {
}
