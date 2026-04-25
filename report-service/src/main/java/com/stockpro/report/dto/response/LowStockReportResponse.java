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
public class LowStockReportResponse {

    private Long productId;
    private String productName;
    private Long warehouseId;
    private BigDecimal availableQuantity;
    private BigDecimal reorderLevel;
}
