package com.stockpro.report.dto.response;

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
public class WarehouseStockValueResponse {

    private Long warehouseId;
    private String warehouseName;
    private BigDecimal totalStockValue;
}
