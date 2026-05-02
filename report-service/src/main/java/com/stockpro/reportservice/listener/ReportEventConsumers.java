package com.stockpro.reportservice.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReportEventConsumers {

    @RabbitListener(queues = "${stockpro.rabbitmq.report.stock-queue}")
    public void onStockEvent(Object payload) {
        log.info("Received stock event for reporting: {}", payload);
    }

    @RabbitListener(queues = "${stockpro.rabbitmq.report.purchase-queue}")
    public void onPurchaseEvent(Object payload) {
        log.info("Received purchase event for reporting: {}", payload);
    }

    @RabbitListener(queues = "${stockpro.rabbitmq.report.payment-queue}")
    public void onPaymentEvent(Object payload) {
        log.info("Received payment event for reporting: {}", payload);
    }

    @RabbitListener(queues = "${stockpro.rabbitmq.report.supplier-queue}")
    public void onSupplierEvent(Object payload) {
        log.info("Received supplier event for reporting: {}", payload);
    }

    @RabbitListener(queues = "${stockpro.rabbitmq.report.movement-queue}")
    public void onMovementEvent(Object payload) {
        log.info("Received movement event for reporting: {}", payload);
    }

    @RabbitListener(queues = "${stockpro.rabbitmq.report.alert-queue}")
    public void onAlertEvent(Object payload) {
        log.info("Received alert event for reporting: {}", payload);
    }
}
