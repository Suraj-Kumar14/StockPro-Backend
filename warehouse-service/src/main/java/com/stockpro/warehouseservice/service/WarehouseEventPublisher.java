package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.events.StockEvent;
import com.stockpro.warehouseservice.events.WarehouseEvent;

public interface WarehouseEventPublisher {
    void publishWarehouseEvent(String routingKey, WarehouseEvent event);
    void publishStockEvent(String routingKey, StockEvent event);
}
