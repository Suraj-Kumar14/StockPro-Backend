package com.stockpro.alertservice.listener;

import com.stockpro.alertservice.events.MovementAlertEvent;
import com.stockpro.alertservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovementAlertEventConsumer {

    private final AlertService alertService;

    @RabbitListener(queues = "${stockpro.rabbitmq.movement.alert.queue}")
    public void consume(MovementAlertEvent event) {
        log.info("Consumed movement alert event eventType={} correlationId={}", event.getEventType(), event.getCorrelationId());
        alertService.createAlertFromMovementEvent(event);
    }
}
