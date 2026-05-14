package com.stockpro.paymentservice.publisher;

import com.stockpro.paymentservice.events.SystemAlertEvent;

public interface SystemAlertPublisher {
    void publish(String routingKey, SystemAlertEvent event);
}
