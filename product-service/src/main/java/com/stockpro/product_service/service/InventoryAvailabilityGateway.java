package com.stockpro.product_service.service;

import java.util.Optional;

public interface InventoryAvailabilityGateway {

    Optional<Integer> getAvailableQuantity(Long productId);

    boolean hasInventoryUsage(Long productId);
}
