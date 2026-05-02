package com.stockpro.alertservice.listener;

import com.stockpro.alertservice.events.StockAlertEvent;
import com.stockpro.alertservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockAlertEventConsumer {

    private final AlertService alertService;

    @RabbitListener(queues = "${stockpro.rabbitmq.warehouse.low.queue}")
    public void consumeLowStock(StockAlertEvent event) {
        event.setEventType(event.getEventType() != null ? event.getEventType() : "LOW_STOCK_DETECTED");
        log.info("Consumed stock low event productId={} warehouseId={}", event.getProductId(), event.getWarehouseId());
        alertService.createAlertFromStockEvent(event);
    }

    @RabbitListener(queues = "${stockpro.rabbitmq.warehouse.overstock.queue}")
    public void consumeOverstock(StockAlertEvent event) {
        event.setEventType(event.getEventType() != null ? event.getEventType() : "OVERSTOCK_DETECTED");
        log.info("Consumed stock overstock event productId={} warehouseId={}", event.getProductId(), event.getWarehouseId());
        alertService.createAlertFromStockEvent(event);
    }
}
