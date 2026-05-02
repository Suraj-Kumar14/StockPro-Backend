package com.stockpro.alertservice.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlertRabbitMQConfig {

    @Bean
    public TopicExchange alertExchange(@Value("${stockpro.rabbitmq.alert.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public TopicExchange warehouseExchange(@Value("${stockpro.rabbitmq.warehouse.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public TopicExchange purchaseExchange(@Value("${stockpro.rabbitmq.purchase.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public TopicExchange supplierExchange(@Value("${stockpro.rabbitmq.supplier.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public TopicExchange movementExchange(@Value("${stockpro.rabbitmq.movement.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue alertEventsQueue(@Value("${stockpro.rabbitmq.alert.events-queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue alertEmailQueue(@Value("${stockpro.rabbitmq.alert.email-queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue alertAuditQueue(@Value("${stockpro.rabbitmq.alert.audit-queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue warehouseLowStockQueue(@Value("${stockpro.rabbitmq.warehouse.low.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue warehouseOverstockQueue(@Value("${stockpro.rabbitmq.warehouse.overstock.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue purchasePendingQueue(@Value("${stockpro.rabbitmq.purchase.pending.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue purchaseApprovedQueue(@Value("${stockpro.rabbitmq.purchase.approved.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue purchaseOverdueQueue(@Value("${stockpro.rabbitmq.purchase.overdue.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue supplierAlertQueue(@Value("${stockpro.rabbitmq.supplier.alert.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Queue movementAlertQueue(@Value("${stockpro.rabbitmq.movement.alert.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding alertEventsBinding(Queue alertEventsQueue, TopicExchange alertExchange) {
        return BindingBuilder.bind(alertEventsQueue).to(alertExchange).with("alert.*");
    }

    @Bean
    public Binding alertEmailBinding(Queue alertEmailQueue, TopicExchange alertExchange) {
        return BindingBuilder.bind(alertEmailQueue).to(alertExchange).with("alert.email.*");
    }

    @Bean
    public Binding alertAuditBinding(Queue alertAuditQueue, TopicExchange alertExchange) {
        return BindingBuilder.bind(alertAuditQueue).to(alertExchange).with("alert.*");
    }

    @Bean
    public Binding warehouseLowStockBinding(
            Queue warehouseLowStockQueue,
            TopicExchange warehouseExchange,
            @Value("${stockpro.rabbitmq.warehouse.low.routing-key}") String routingKey) {
        return BindingBuilder.bind(warehouseLowStockQueue).to(warehouseExchange).with(routingKey);
    }

    @Bean
    public Binding warehouseOverstockBinding(
            Queue warehouseOverstockQueue,
            TopicExchange warehouseExchange,
            @Value("${stockpro.rabbitmq.warehouse.overstock.routing-key}") String routingKey) {
        return BindingBuilder.bind(warehouseOverstockQueue).to(warehouseExchange).with(routingKey);
    }

    @Bean
    public Binding purchasePendingBinding(
            Queue purchasePendingQueue,
            TopicExchange purchaseExchange,
            @Value("${stockpro.rabbitmq.purchase.pending.routing-key}") String routingKey) {
        return BindingBuilder.bind(purchasePendingQueue).to(purchaseExchange).with(routingKey);
    }

    @Bean
    public Binding purchaseApprovedBinding(
            Queue purchaseApprovedQueue,
            TopicExchange purchaseExchange,
            @Value("${stockpro.rabbitmq.purchase.approved.routing-key}") String routingKey) {
        return BindingBuilder.bind(purchaseApprovedQueue).to(purchaseExchange).with(routingKey);
    }

    @Bean
    public Binding purchaseOverdueBinding(
            Queue purchaseOverdueQueue,
            TopicExchange purchaseExchange,
            @Value("${stockpro.rabbitmq.purchase.overdue.routing-key}") String routingKey) {
        return BindingBuilder.bind(purchaseOverdueQueue).to(purchaseExchange).with(routingKey);
    }

    @Bean
    public Binding supplierDeactivatedBinding(
            Queue supplierAlertQueue,
            TopicExchange supplierExchange,
            @Value("${stockpro.rabbitmq.supplier.alert.deactivated-routing-key}") String routingKey) {
        return BindingBuilder.bind(supplierAlertQueue).to(supplierExchange).with(routingKey);
    }

    @Bean
    public Binding supplierBlacklistedBinding(
            Queue supplierAlertQueue,
            TopicExchange supplierExchange,
            @Value("${stockpro.rabbitmq.supplier.alert.blacklisted-routing-key}") String routingKey) {
        return BindingBuilder.bind(supplierAlertQueue).to(supplierExchange).with(routingKey);
    }

    @Bean
    public Binding movementAlertBinding(
            Queue movementAlertQueue,
            TopicExchange movementExchange,
            @Value("${stockpro.rabbitmq.movement.alert.routing-key}") String routingKey) {
        return BindingBuilder.bind(movementAlertQueue).to(movementExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
