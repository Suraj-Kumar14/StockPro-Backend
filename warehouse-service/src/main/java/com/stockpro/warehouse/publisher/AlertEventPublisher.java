package com.stockpro.warehouse.publisher;

import com.stockpro.warehouse.dto.event.LowStockEvent;
import com.stockpro.warehouse.dto.event.OverstockEvent;

public interface AlertEventPublisher {

    void publishLowStockEvent(LowStockEvent event);

    void publishOverstockEvent(OverstockEvent event);
}
