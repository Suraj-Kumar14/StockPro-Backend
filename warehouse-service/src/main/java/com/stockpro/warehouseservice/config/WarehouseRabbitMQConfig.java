package com.stockpro.warehouseservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WarehouseRabbitMQConfig {

    @Bean
    TopicExchange warehouseExchange(@Value("${stockpro.rabbitmq.warehouse.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean Queue warehouseEventsQueue() { return new Queue("stockpro.warehouse.events.queue", true); }
    @Bean Queue stockEventsQueue() { return new Queue("stockpro.stock.events.queue", true); }
    @Bean Queue stockAlertQueue() { return new Queue("stockpro.stock.alert.queue", true); }
    @Bean Queue stockReportQueue() { return new Queue("stockpro.stock.report.queue", true); }
    @Bean Queue stockMovementQueue() { return new Queue("stockpro.stock.movement.queue", true); }

    @Bean Binding warehouseBinding(Queue warehouseEventsQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(warehouseEventsQueue).to(warehouseExchange).with("warehouse.*");
    }

    @Bean Binding stockBinding(Queue stockEventsQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockEventsQueue).to(warehouseExchange).with("stock.*");
    }

    @Bean Binding lowStockAlertBinding(Queue stockAlertQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockAlertQueue).to(warehouseExchange).with("stock.low");
    }

    @Bean Binding overstockAlertBinding(Queue stockAlertQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockAlertQueue).to(warehouseExchange).with("stock.overstock");
    }

    @Bean Binding stockReportBinding(Queue stockReportQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockReportQueue).to(warehouseExchange).with("stock.*");
    }

    @Bean Binding stockReceivedBinding(Queue stockMovementQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockMovementQueue).to(warehouseExchange).with("stock.received");
    }

    @Bean Binding stockIssuedBinding(Queue stockMovementQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockMovementQueue).to(warehouseExchange).with("stock.issued");
    }

    @Bean Binding stockTransferredBinding(Queue stockMovementQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockMovementQueue).to(warehouseExchange).with("stock.transferred");
    }

    @Bean Binding stockAdjustedBinding(Queue stockMovementQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockMovementQueue).to(warehouseExchange).with("stock.adjusted");
    }

    @Bean
    MessageConverter warehouseMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
