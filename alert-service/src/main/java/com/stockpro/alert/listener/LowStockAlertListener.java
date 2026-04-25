package com.stockpro.alert.listener;

import com.stockpro.alert.dto.event.LowStockEvent;
import com.stockpro.alert.exception.AlertEventProcessingException;
import com.stockpro.alert.service.AlertService;
import com.stockpro.alert.validation.AlertEventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LowStockAlertListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LowStockAlertListener.class);

    private final AlertService alertService;
    private final AlertEventValidator alertEventValidator;

    public LowStockAlertListener(AlertService alertService, AlertEventValidator alertEventValidator) {
        this.alertService = alertService;
        this.alertEventValidator = alertEventValidator;
    }

    @RabbitListener(queues = "${alert.rabbitmq.queues.low-stock}")
    public void handle(LowStockEvent event) {
        try {
            alertEventValidator.validate(event);
            LOGGER.info("Consumed low-stock event recipientId={} productId={} warehouseId={}",
                    event.getRecipientId(),
                    event.getProductId(),
                    event.getWarehouseId());
            alertService.handleLowStockEvent(event);
        } catch (Exception exception) {
            throw new AlertEventProcessingException("Failed to process low-stock event.", exception);
        }
    }
}
