package com.stockpro.purchaseservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdatePurchaseOrderRequest(
        @NotNull Long supplierId,
        @NotNull Long warehouseId,
        @NotNull LocalDate expectedDeliveryDate,
        String paymentTerms,
        String notes,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal taxAmount,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal discountAmount,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal shippingAmount,
        @Valid @NotEmpty List<CreatePurchaseOrderLineItemRequest> lineItems) {
}
