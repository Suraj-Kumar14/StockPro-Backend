package com.stockpro.alert.listener;

import com.stockpro.alert.dto.event.OverstockEvent;
import com.stockpro.alert.exception.AlertEventProcessingException;
import com.stockpro.alert.service.AlertService;
import com.stockpro.alert.validation.AlertEventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OverstockAlertListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverstockAlertListener.class);

    private final AlertService alertService;
    private final AlertEventValidator alertEventValidator;

    public OverstockAlertListener(AlertService alertService, AlertEventValidator alertEventValidator) {
        this.alertService = alertService;
        this.alertEventValidator = alertEventValidator;
    }

    @RabbitListener(queues = "${alert.rabbitmq.queues.overstock}")
    public void handle(OverstockEvent event) {
        try {
            alertEventValidator.validate(event);
            LOGGER.info("Consumed overstock event recipientId={} productId={} warehouseId={}",
                    event.getRecipientId(),
                    event.getProductId(),
                    event.getWarehouseId());
            alertService.handleOverstockEvent(event);
        } catch (Exception exception) {
            throw new AlertEventProcessingException("Failed to process overstock event.", exception);
        }
    }
}
