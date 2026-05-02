package com.stockpro.purchaseservice.dto;

import lombok.Data;

@Data
public class WarehouseLookupResponseDTO {

    private Long warehouseId;
    private String name;
    private Boolean isActive;
}
