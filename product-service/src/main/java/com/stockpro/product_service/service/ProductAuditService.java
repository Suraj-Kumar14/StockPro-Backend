package com.stockpro.product_service.service;

public interface ProductAuditService {

    void record(ProductAuditEntry entry);
}
