package com.stockpro.alertservice.rabbitmq;

import com.stockpro.alertservice.events.AlertEvent;

public interface AlertEventPublisher {
    void publish(String routingKey, AlertEvent event);
}
