package com.stockpro.movement.dto.response;

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
public class StockInOutSummaryResponse {

    private Long productId;
    private BigDecimal totalStockIn;
    private BigDecimal totalStockOut;
}
