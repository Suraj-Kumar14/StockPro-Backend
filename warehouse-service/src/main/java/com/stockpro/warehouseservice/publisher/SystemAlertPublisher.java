package com.stockpro.warehouseservice.publisher;

import com.stockpro.warehouseservice.events.SystemAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAlertPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.alert.exchange}")
    private String exchange;

    public void publish(String routingKey, SystemAlertEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published warehouse-service system alert routingKey={} title={}", routingKey, event.title());
        } catch (Exception ex) {
            log.error("Failed to publish warehouse-service system alert routingKey={} error={}", routingKey, ex.getMessage(), ex);
        }
    }
}
