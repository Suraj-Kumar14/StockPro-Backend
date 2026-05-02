package com.stockpro.warehouseservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class StockReceiptRequest {
    @NotNull private Long warehouseId;
    @NotNull private Long productId;
    @Min(1) private Integer quantity;
    private String referenceId;
    private String referenceType;
    @DecimalMin(value = "0.0", inclusive = true) private BigDecimal unitCost;
    private String notes;
}
