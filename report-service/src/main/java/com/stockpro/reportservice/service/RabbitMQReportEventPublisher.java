package com.stockpro.reportservice.service;

import com.stockpro.reportservice.events.ReportEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQReportEventPublisher implements ReportEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.report.exchange}")
    private String exchange;

    @Override
    public void publish(String routingKey, ReportEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published report event {} with routing key {}", event.eventType(), routingKey);
        } catch (Exception ex) {
            log.warn("Failed to publish report event {}: {}", event.eventType(), ex.getMessage());
        }
    }
}
