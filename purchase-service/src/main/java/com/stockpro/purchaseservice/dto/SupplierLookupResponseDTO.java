package com.stockpro.purchaseservice.dto;

import lombok.Data;

@Data
public class SupplierLookupResponseDTO {

    private Long supplierId;
    private String name;
    private String paymentTerms;
    private Boolean isActive;
}
