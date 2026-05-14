package com.stockpro.alertservice.listener;

import com.stockpro.alertservice.events.SupplierAlertEvent;
import com.stockpro.alertservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupplierAlertEventConsumer {

    private final AlertService alertService;

    @RabbitListener(queues = "${stockpro.rabbitmq.supplier.alert.queue}")
    public void consume(SupplierAlertEvent event) {
        log.info("Consumed supplier alert event eventType={} correlationId={}", event.getEventType(), event.getCorrelationId());
        alertService.createAlertFromSupplierEvent(event);
    }
}
