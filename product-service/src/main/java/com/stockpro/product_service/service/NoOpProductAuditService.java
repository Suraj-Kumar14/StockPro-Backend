package com.stockpro.product_service.service;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NoOpProductAuditService implements ProductAuditService {

    @Override
    public void record(ProductAuditEntry entry) {
        log.info(
                "Product audit placeholder - actorId={}, action={}, entityId={}, serviceName={}",
                entry.actorId(),
                entry.action(),
                entry.entityId(),
                entry.serviceName());
    }
}
