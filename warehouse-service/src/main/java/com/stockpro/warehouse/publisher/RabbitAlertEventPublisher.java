package com.stockpro.warehouse.publisher;

import com.stockpro.warehouse.config.AlertRabbitMqProperties;
import com.stockpro.warehouse.dto.event.LowStockEvent;
import com.stockpro.warehouse.dto.event.OverstockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitAlertEventPublisher implements AlertEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitAlertEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final AlertRabbitMqProperties properties;

    public RabbitAlertEventPublisher(RabbitTemplate rabbitTemplate, AlertRabbitMqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publishLowStockEvent(LowStockEvent event) {
        publish(properties.getRoutingKeys().getLowStock(), event, "low-stock");
    }

    @Override
    public void publishOverstockEvent(OverstockEvent event) {
        publish(properties.getRoutingKeys().getOverstock(), event, "overstock");
    }

    private void publish(String routingKey, Object event, String type) {
        try {
            rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, event);
            LOGGER.info("Published {} alert event to exchange={} routingKey={}", type, properties.getExchange(), routingKey);
        } catch (AmqpException exception) {
            LOGGER.error("Failed to publish {} alert event: {}", type, exception.getMessage(), exception);
        }
    }
}
