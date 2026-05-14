package com.stockpro.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqOtpEventPublisher implements OtpEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.auth.exchange}")
    private String exchange;

    @Value("${stockpro.rabbitmq.auth.otp.routing-key}")
    private String routingKey;

    @Override
    public boolean publish(OtpNotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("OTP event published email={} purpose={} eventId={}",
                    event.email(), event.purpose(), event.eventId());
            return true;
        } catch (AmqpException ex) {
            log.error("Failed to publish OTP event email={} purpose={} error={}",
                    event.email(), event.purpose(), ex.getMessage(), ex);
            return false;
        }
    }
}
