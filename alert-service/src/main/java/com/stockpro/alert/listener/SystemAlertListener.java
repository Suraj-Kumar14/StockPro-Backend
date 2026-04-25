package com.stockpro.alert.listener;

import com.stockpro.alert.dto.event.SystemAlertEvent;
import com.stockpro.alert.exception.AlertEventProcessingException;
import com.stockpro.alert.service.AlertService;
import com.stockpro.alert.validation.AlertEventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SystemAlertListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemAlertListener.class);

    private final AlertService alertService;
    private final AlertEventValidator alertEventValidator;

    public SystemAlertListener(AlertService alertService, AlertEventValidator alertEventValidator) {
        this.alertService = alertService;
        this.alertEventValidator = alertEventValidator;
    }

    @RabbitListener(queues = "${alert.rabbitmq.queues.system}")
    public void handle(SystemAlertEvent event) {
        try {
            alertEventValidator.validate(event);
            LOGGER.info("Consumed system alert event recipientId={} severity={}",
                    event.getRecipientId(),
                    event.getSeverity());
            alertService.handleSystemAlertEvent(event);
        } catch (Exception exception) {
            throw new AlertEventProcessingException("Failed to process system alert event.", exception);
        }
    }
}
