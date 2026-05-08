package com.stockpro.purchaseservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class WarehouseStockUpdateDTO {

    private Long warehouseId;
    private Long productId;
    private Integer quantity;
    private String movementType;
    private String binLocation;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private String referenceId;
    private String referenceType;
    private String reason;
    private String notes;
    private BigDecimal unitCost;
}
