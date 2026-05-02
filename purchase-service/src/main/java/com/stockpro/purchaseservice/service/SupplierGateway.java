package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.SupplierLookupResponseDTO;

public interface SupplierGateway {

    void ensureSupplierExists(Long supplierId);

    SupplierLookupResponseDTO getSupplier(Long supplierId);
}
