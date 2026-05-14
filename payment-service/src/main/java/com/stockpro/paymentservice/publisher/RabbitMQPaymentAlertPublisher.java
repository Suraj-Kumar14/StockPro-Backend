package com.stockpro.paymentservice.publisher;

import com.stockpro.paymentservice.events.PaymentAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPaymentAlertPublisher implements PaymentAlertPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.payment.exchange}")
    private String exchange;

    @Override
    public void publish(String routingKey, PaymentAlertEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published payment alert event routingKey={} paymentId={}", routingKey, event.paymentId());
        } catch (Exception ex) {
            log.error("Failed to publish payment alert event routingKey={} paymentId={} error={}",
                    routingKey, event.paymentId(), ex.getMessage(), ex);
        }
    }
}
