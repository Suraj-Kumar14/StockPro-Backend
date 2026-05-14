package com.stockpro.supplierservice.service;

import com.stockpro.supplierservice.events.SupplierEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQSupplierEventPublisher implements SupplierEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.supplier.exchange}")
    private String exchange;

    @Override
    public void publish(String routingKey, SupplierEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published supplier event {} with routing key {}", event.eventType(), routingKey);
        } catch (Exception ex) {
            log.error("Failed to publish supplier event {} with routing key {}", event.eventType(), routingKey, ex);
        }
    }
}
