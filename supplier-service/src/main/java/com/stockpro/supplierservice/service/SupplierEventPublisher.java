package com.stockpro.supplierservice.service;

import com.stockpro.supplierservice.events.SupplierEvent;

public interface SupplierEventPublisher {
    void publish(String routingKey, SupplierEvent event);
}
