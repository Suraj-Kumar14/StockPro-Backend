package com.stockpro.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class StockMovementSummaryResponse {

    private Long productId;
    private Long warehouseId;
    private BigDecimal stockIn;
    private BigDecimal stockOut;
    private BigDecimal adjustment;
    private BigDecimal transferIn;
    private BigDecimal transferOut;
    private LocalDate fromDate;
    private LocalDate toDate;
}
