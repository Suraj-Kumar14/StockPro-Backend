package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaseOrderSummaryResponse {

    private Long supplierId;
    private Long warehouseId;
    private Long totalPOs;
    private BigDecimal totalSpend;
    private LocalDate fromDate;
    private LocalDate toDate;
}
