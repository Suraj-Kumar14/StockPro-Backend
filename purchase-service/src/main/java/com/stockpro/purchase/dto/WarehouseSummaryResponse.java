package com.stockpro.purchase.dto;

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
public class WarehouseSummaryResponse {

    private Long warehouseId;
    private String name;
    private Long managerId;
    private Boolean isActive;
}
