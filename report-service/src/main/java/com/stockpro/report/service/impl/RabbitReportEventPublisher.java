package com.stockpro.report.service.impl;

import com.stockpro.report.config.ReportRabbitMqProperties;
import com.stockpro.report.dto.event.InventorySnapshotCompletedEvent;
import com.stockpro.report.dto.event.ReportGenerationCompletedEvent;
import com.stockpro.report.dto.event.ReportGenerationRequestedEvent;
import com.stockpro.report.service.ReportEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitReportEventPublisher implements ReportEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitReportEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ReportRabbitMqProperties properties;

    public RabbitReportEventPublisher(RabbitTemplate rabbitTemplate, ReportRabbitMqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publishSnapshotCompleted(InventorySnapshotCompletedEvent event) {
        publish(properties.getRoutingKeys().getSnapshotCompleted(), event, "snapshot-completed");
    }

    @Override
    public void publishReportGenerationRequested(ReportGenerationRequestedEvent event) {
        publish(properties.getRoutingKeys().getGenerationRequested(), event, "generation-requested");
    }

    @Override
    public void publishReportGenerationCompleted(ReportGenerationCompletedEvent event) {
        publish(properties.getRoutingKeys().getGenerationCompleted(), event, "generation-completed");
    }

    private void publish(String routingKey, Object payload, String eventType) {
        try {
            rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, payload);
            LOGGER.info("Published report event type={} exchange={} routingKey={}",
                    eventType,
                    properties.getExchange(),
                    routingKey);
        } catch (AmqpException exception) {
            LOGGER.error("Failed to publish report event type={}: {}", eventType, exception.getMessage(), exception);
        }
    }
}
