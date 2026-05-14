package com.stockpro.paymentservice.publisher;

import com.stockpro.paymentservice.events.PaymentAlertEvent;

public interface PaymentAlertPublisher {
    void publish(String routingKey, PaymentAlertEvent event);
}
