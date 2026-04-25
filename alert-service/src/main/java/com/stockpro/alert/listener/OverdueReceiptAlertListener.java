package com.stockpro.alert.listener;

import com.stockpro.alert.dto.event.OverdueReceiptEvent;
import com.stockpro.alert.exception.AlertEventProcessingException;
import com.stockpro.alert.service.AlertService;
import com.stockpro.alert.validation.AlertEventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OverdueReceiptAlertListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverdueReceiptAlertListener.class);

    private final AlertService alertService;
    private final AlertEventValidator alertEventValidator;

    public OverdueReceiptAlertListener(AlertService alertService, AlertEventValidator alertEventValidator) {
        this.alertService = alertService;
        this.alertEventValidator = alertEventValidator;
    }

    @RabbitListener(queues = "${alert.rabbitmq.queues.overdue-receipt}")
    public void handle(OverdueReceiptEvent event) {
        try {
            alertEventValidator.validate(event);
            LOGGER.info("Consumed overdue receipt event recipientId={} purchaseOrderId={}",
                    event.getRecipientId(),
                    event.getPurchaseOrderId());
            alertService.handleOverdueReceiptEvent(event);
        } catch (Exception exception) {
            throw new AlertEventProcessingException("Failed to process overdue receipt event.", exception);
        }
    }
}
