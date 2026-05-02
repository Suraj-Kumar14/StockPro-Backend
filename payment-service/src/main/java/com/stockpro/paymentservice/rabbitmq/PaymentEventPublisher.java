package com.stockpro.paymentservice.rabbitmq;

import com.stockpro.paymentservice.events.PaymentEvent;

public interface PaymentEventPublisher {
    void publish(String routingKey, PaymentEvent event);
}
