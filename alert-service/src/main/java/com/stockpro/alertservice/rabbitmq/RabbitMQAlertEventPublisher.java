package com.stockpro.alertservice.rabbitmq;

import com.stockpro.alertservice.events.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQAlertEventPublisher implements AlertEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.alert.exchange}")
    private String exchange;

    @Override
    public void publish(String routingKey, AlertEvent event) {
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Published alert event routingKey={} alertId={}", routingKey, event.getAlertId());
    }
}
