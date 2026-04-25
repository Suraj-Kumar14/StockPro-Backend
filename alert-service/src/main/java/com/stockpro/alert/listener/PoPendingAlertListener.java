package com.stockpro.alert.listener;

import com.stockpro.alert.dto.event.PoPendingApprovalEvent;
import com.stockpro.alert.exception.AlertEventProcessingException;
import com.stockpro.alert.service.AlertService;
import com.stockpro.alert.validation.AlertEventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PoPendingAlertListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoPendingAlertListener.class);

    private final AlertService alertService;
    private final AlertEventValidator alertEventValidator;

    public PoPendingAlertListener(AlertService alertService, AlertEventValidator alertEventValidator) {
        this.alertService = alertService;
        this.alertEventValidator = alertEventValidator;
    }

    @RabbitListener(queues = "${alert.rabbitmq.queues.po-pending}")
    public void handle(PoPendingApprovalEvent event) {
        try {
            alertEventValidator.validate(event);
            LOGGER.info("Consumed PO pending event recipientId={} purchaseOrderId={}",
                    event.getRecipientId(),
                    event.getPurchaseOrderId());
            alertService.handlePoPendingApprovalEvent(event);
        } catch (Exception exception) {
            throw new AlertEventProcessingException("Failed to process PO pending event.", exception);
        }
    }
}
