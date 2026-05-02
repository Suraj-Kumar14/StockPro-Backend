package com.stockpro.paymentservice.client;

public interface SupplierServiceClient {
    SupplierLookupResponse getSupplier(Long supplierId);
}
