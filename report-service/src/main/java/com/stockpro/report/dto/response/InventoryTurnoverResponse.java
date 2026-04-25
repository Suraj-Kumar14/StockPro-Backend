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
public class InventoryTurnoverResponse {

    private Long productId;
    private String productName;
    private BigDecimal turnoverRate;
    private LocalDate fromDate;
    private LocalDate toDate;
}
