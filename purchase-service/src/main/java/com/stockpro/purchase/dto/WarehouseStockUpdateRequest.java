package com.stockpro.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockUpdateRequest {

    private Integer quantityChange;
    private String movementType;
    private Long referenceId;
    private String referenceType;
    private String notes;
}
