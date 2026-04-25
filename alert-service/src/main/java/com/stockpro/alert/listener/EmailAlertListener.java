package com.stockpro.alert.listener;

import com.stockpro.alert.dto.event.EmailAlertEvent;
import com.stockpro.alert.exception.AlertEventProcessingException;
import com.stockpro.alert.mail.MailService;
import com.stockpro.alert.validation.AlertEventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailAlertListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailAlertListener.class);

    private final MailService mailService;
    private final AlertEventValidator alertEventValidator;

    public EmailAlertListener(MailService mailService, AlertEventValidator alertEventValidator) {
        this.mailService = mailService;
        this.alertEventValidator = alertEventValidator;
    }

    @RabbitListener(queues = "${alert.rabbitmq.queues.email}")
    public void handle(EmailAlertEvent event) {
        try {
            alertEventValidator.validate(event);
            LOGGER.info("Consumed email alert event alertId={} recipientId={}", event.getAlertId(), event.getRecipientId());
            mailService.sendEmail(event.getToEmail(), event.getSubject(), event.getBody());
        } catch (Exception exception) {
            throw new AlertEventProcessingException("Failed to process email alert event.", exception);
        }
    }
}
