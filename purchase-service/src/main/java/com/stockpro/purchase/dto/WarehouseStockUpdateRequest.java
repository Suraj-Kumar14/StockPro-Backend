package com.stockpro.purchase.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStockUpdateRequest {

    private Long warehouseId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private Long referenceId;
    private String referenceType;
    private String location;
    private String notes;
}
