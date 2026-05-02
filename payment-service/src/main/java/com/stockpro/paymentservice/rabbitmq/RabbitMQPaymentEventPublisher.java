package com.stockpro.paymentservice.rabbitmq;

import com.stockpro.paymentservice.events.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPaymentEventPublisher implements PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.payment.exchange}")
    private String exchange;

    @Override
    public void publish(String routingKey, PaymentEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published payment event paymentId={} routingKey={}", event.paymentId(), routingKey);
        } catch (Exception ex) {
            log.error("Failed to publish payment event paymentId={} routingKey={} error={}",
                    event.paymentId(), routingKey, ex.getMessage(), ex);
        }
    }
}
