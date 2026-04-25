package com.stockpro.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferStockRequest {

    @NotNull(message = "Source warehouse is required.")
    @Positive(message = "Source warehouse is invalid.")
    private Long sourceWarehouseId;

    @NotNull(message = "Destination warehouse is required.")
    @Positive(message = "Destination warehouse is invalid.")
    private Long destinationWarehouseId;

    @NotNull(message = "Product is required.")
    @Positive(message = "Product is invalid.")
    private Long productId;

    @NotNull(message = "Quantity is required.")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero.")
    private BigDecimal quantity;

    private Long referenceId;
    private String referenceType = "WAREHOUSE_TRANSFER";
    private BigDecimal unitCost;
    private String notes;
}
