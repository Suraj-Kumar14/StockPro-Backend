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
public class SupplierSummaryResponse {

    private Long supplierId;
    private String supplierName;
    private Boolean isActive;
}
