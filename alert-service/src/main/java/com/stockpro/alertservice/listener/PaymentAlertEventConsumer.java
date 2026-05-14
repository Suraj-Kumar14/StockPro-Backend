package com.stockpro.alertservice.listener;

import com.stockpro.alertservice.events.PaymentAlertEvent;
import com.stockpro.alertservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentAlertEventConsumer {

    private final AlertService alertService;

    @RabbitListener(queues = "${stockpro.rabbitmq.payment.alert.queue}")
    public void consume(PaymentAlertEvent event) {
        log.info("Consumed payment alert event paymentId={} eventType={}", event.getPaymentId(), event.getEventType());
        alertService.createAlertFromPaymentEvent(event);
    }
}
