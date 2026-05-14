package com.stockpro.alertservice.listener;

import com.stockpro.alertservice.events.SystemAlertEvent;
import com.stockpro.alertservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemAlertEventConsumer {

    private final AlertService alertService;

    @RabbitListener(queues = "${stockpro.rabbitmq.alert.system.queue}")
    public void consume(SystemAlertEvent event) {
        log.info("Consumed system alert event title={} sourceService={}", event.getTitle(), event.getSourceService());
        alertService.createAlertFromSystemEvent(event);
    }
}
