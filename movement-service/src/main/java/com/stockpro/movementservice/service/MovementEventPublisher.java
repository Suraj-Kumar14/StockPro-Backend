package com.stockpro.movementservice.service;

import com.stockpro.movementservice.events.MovementEvent;

public interface MovementEventPublisher {

    void publish(String routingKey, MovementEvent event);
}
