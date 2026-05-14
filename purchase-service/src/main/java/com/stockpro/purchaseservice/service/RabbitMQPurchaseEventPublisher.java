package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.events.PurchaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPurchaseEventPublisher implements PurchaseEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.purchase.exchange}")
    private String exchange;

    @Override
    public void publish(String routingKey, PurchaseEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published purchase event: routingKey={}", routingKey);
        } catch (Exception ex) {
            log.error("Failed to publish purchase event: routingKey={}, error={}", routingKey, ex.getMessage());
        }
    }
}
