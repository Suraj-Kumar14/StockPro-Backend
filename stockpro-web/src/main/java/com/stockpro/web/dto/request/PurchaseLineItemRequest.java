package com.stockpro.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseLineItemRequest {

    private Long lineItemId;

    @NotNull(message = "Product is required.")
    @Positive(message = "Product is invalid.")
    private Long productId;

    @NotNull(message = "Quantity is required.")
    @Positive(message = "Quantity must be greater than zero.")
    private Integer quantity;

    @NotNull(message = "Unit cost is required.")
    @DecimalMin(value = "0.0", message = "Unit cost cannot be negative.")
    private BigDecimal unitCost;

    @DecimalMin(value = "0.0", message = "Total cost cannot be negative.")
    private BigDecimal totalCost = BigDecimal.ZERO;

    @PositiveOrZero(message = "Received quantity cannot be negative.")
    private Integer receivedQty = 0;
}
