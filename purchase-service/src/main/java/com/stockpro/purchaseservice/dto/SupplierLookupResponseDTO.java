package com.stockpro.purchaseservice.dto;

import lombok.Data;

@Data
public class SupplierLookupResponseDTO {

    private Long supplierId;
    private Boolean isActive;
}
