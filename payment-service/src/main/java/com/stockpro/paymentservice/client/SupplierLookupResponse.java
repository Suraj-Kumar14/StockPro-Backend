package com.stockpro.paymentservice.client;

import lombok.Data;

@Data
public class SupplierLookupResponse {
    private Long supplierId;
    private String name;
    private Boolean isActive;
    private String status;
    private String paymentTerms;
}
