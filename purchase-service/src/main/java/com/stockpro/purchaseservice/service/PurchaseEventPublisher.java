package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.events.PurchaseEvent;

public interface PurchaseEventPublisher {
    void publish(String routingKey, PurchaseEvent event);
}
