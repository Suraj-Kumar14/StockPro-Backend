package com.stockpro.product_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NoOpPurchaseOrderUsageGateway implements PurchaseOrderUsageGateway {

    @Override
    public boolean hasPurchaseOrderUsage(Long productId) {
        log.debug("Purchase-order usage check is running in placeholder mode for product {}",
                productId);
        return false;
    }
}
