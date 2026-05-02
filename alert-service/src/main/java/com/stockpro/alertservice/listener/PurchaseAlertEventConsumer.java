package com.stockpro.alertservice.listener;

import com.stockpro.alertservice.events.PurchaseAlertEvent;
import com.stockpro.alertservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseAlertEventConsumer {

    private final AlertService alertService;

    @RabbitListener(queues = "${stockpro.rabbitmq.purchase.pending.queue}")
    public void consumePending(PurchaseAlertEvent event) {
        event.setEventType(event.getEventType() != null ? event.getEventType() : "PURCHASE_ORDER_PENDING_APPROVAL");
        log.info("Consumed purchase pending event purchaseOrderId={}", event.getPurchaseOrderId());
        alertService.createAlertFromPurchaseEvent(event);
    }

    @RabbitListener(queues = "${stockpro.rabbitmq.purchase.approved.queue}")
    public void consumeApproved(PurchaseAlertEvent event) {
        event.setEventType(event.getEventType() != null ? event.getEventType() : "PURCHASE_ORDER_APPROVED");
        log.info("Consumed purchase approved event purchaseOrderId={}", event.getPurchaseOrderId());
        alertService.createAlertFromPurchaseEvent(event);
    }

    @RabbitListener(queues = "${stockpro.rabbitmq.purchase.overdue.queue}")
    public void consumeOverdue(PurchaseAlertEvent event) {
        event.setEventType(event.getEventType() != null ? event.getEventType() : "PURCHASE_ORDER_OVERDUE");
        log.info("Consumed purchase overdue event purchaseOrderId={}", event.getPurchaseOrderId());
        alertService.createAlertFromPurchaseEvent(event);
    }
}
