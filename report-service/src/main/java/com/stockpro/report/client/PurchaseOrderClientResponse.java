package com.stockpro.report.client;

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
public class PurchaseOrderClientResponse {

    private Long poId;
    private Long supplierId;
    private Long warehouseId;
    private Long createdById;
    private String status;
    private BigDecimal totalAmount;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private LocalDate receivedDate;
    private String notes;
    private String referenceNumber;
}
