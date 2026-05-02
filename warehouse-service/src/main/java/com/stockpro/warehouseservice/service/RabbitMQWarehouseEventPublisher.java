package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.events.StockEvent;
import com.stockpro.warehouseservice.events.WarehouseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQWarehouseEventPublisher implements WarehouseEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.warehouse.exchange}")
    private String exchange;

    @Override
    public void publishWarehouseEvent(String routingKey, WarehouseEvent event) {
        publish(routingKey, event);
    }

    @Override
    public void publishStockEvent(String routingKey, StockEvent event) {
        publish(routingKey, event);
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("Published RabbitMQ event: routingKey={}", routingKey);
        } catch (Exception ex) {
            log.error("Failed to publish RabbitMQ event: routingKey={}, error={}", routingKey, ex.getMessage());
        }
    }
}
