package com.stockpro.alert.publisher;

import com.stockpro.alert.config.AlertRabbitMqProperties;
import com.stockpro.alert.dto.event.EmailAlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public void publishEmailAlert(EmailAlertEvent event) {
        LOGGER.info("Publishing email alert event alertId={} recipientId={}", event.getAlertId(), event.getRecipientId());
        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                properties.getRoutingKeys().getEmail(),
                event);
    }
}
