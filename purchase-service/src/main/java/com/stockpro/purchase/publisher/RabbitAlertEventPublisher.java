package com.stockpro.purchase.publisher;

import com.stockpro.purchase.config.AlertRabbitMqProperties;
import com.stockpro.purchase.dto.event.OverdueReceiptEvent;
import com.stockpro.purchase.dto.event.PoPendingApprovalEvent;
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
    public void publishPoPendingApprovalEvent(PoPendingApprovalEvent event) {
        publish(properties.getRoutingKeys().getPoPending(), event, "po-pending");
    }

    @Override
    public void publishOverdueReceiptEvent(OverdueReceiptEvent event) {
        publish(properties.getRoutingKeys().getOverdueReceipt(), event, "overdue-receipt");
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
