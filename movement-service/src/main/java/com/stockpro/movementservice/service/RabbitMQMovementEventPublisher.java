package com.stockpro.movementservice.service;

import com.stockpro.movementservice.events.MovementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQMovementEventPublisher implements MovementEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.movement.exchange}")
    private String exchange;

    @Override
    public void publish(String routingKey, MovementEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published movement event routingKey={} movementId={}", routingKey, event.movementId());
        } catch (Exception ex) {
            log.error("Failed to publish movement event routingKey={} movementId={} error={}",
                    routingKey, event.movementId(), ex.getMessage());
        }
    }
}
