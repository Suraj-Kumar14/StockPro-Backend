package com.stockpro.paymentservice.publisher;

import com.stockpro.paymentservice.events.SystemAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQSystemAlertPublisher implements SystemAlertPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.alert.exchange}")
    private String exchange;

    @Override
    public void publish(String routingKey, SystemAlertEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published system alert event routingKey={} sourceService={}", routingKey, event.sourceService());
        } catch (Exception ex) {
            log.error("Failed to publish system alert event routingKey={} sourceService={} error={}",
                    routingKey, event.sourceService(), ex.getMessage(), ex);
        }
    }
}
