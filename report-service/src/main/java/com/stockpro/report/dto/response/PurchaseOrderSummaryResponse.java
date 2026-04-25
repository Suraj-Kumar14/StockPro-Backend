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
public class PurchaseOrderSummaryResponse {

    private Long supplierId;
    private Long warehouseId;
    private Long totalPOs;
    private BigDecimal totalSpend;
    private LocalDate fromDate;
    private LocalDate toDate;
}
