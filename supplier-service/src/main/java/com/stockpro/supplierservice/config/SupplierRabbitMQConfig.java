package com.stockpro.supplierservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SupplierRabbitMQConfig {

    @Bean
    TopicExchange supplierExchange(@Value("${stockpro.rabbitmq.supplier.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean Queue supplierEventsQueue() { return QueueBuilder.durable("stockpro.supplier.events.queue").build(); }
    @Bean Queue supplierReportQueue() { return QueueBuilder.durable("stockpro.supplier.report.queue").build(); }
    @Bean Queue supplierPurchaseQueue() { return QueueBuilder.durable("stockpro.supplier.purchase.queue").build(); }

    @Bean
    Binding supplierEventsBinding(Queue supplierEventsQueue, TopicExchange supplierExchange) {
        return BindingBuilder.bind(supplierEventsQueue).to(supplierExchange).with("supplier.*");
    }

    @Bean
    Binding supplierCreatedBinding(Queue supplierPurchaseQueue, TopicExchange supplierExchange) {
        return BindingBuilder.bind(supplierPurchaseQueue).to(supplierExchange).with("supplier.created");
    }

    @Bean
    Binding supplierUpdatedBinding(Queue supplierPurchaseQueue, TopicExchange supplierExchange) {
        return BindingBuilder.bind(supplierPurchaseQueue).to(supplierExchange).with("supplier.updated");
    }

    @Bean
    Binding supplierActivatedBinding(Queue supplierPurchaseQueue, TopicExchange supplierExchange) {
        return BindingBuilder.bind(supplierPurchaseQueue).to(supplierExchange).with("supplier.activated");
    }

    @Bean
    Binding supplierDeactivatedBinding(Queue supplierPurchaseQueue, TopicExchange supplierExchange) {
        return BindingBuilder.bind(supplierPurchaseQueue).to(supplierExchange).with("supplier.deactivated");
    }

    @Bean
    Binding supplierBlacklistedBinding(Queue supplierPurchaseQueue, TopicExchange supplierExchange) {
        return BindingBuilder.bind(supplierPurchaseQueue).to(supplierExchange).with("supplier.blacklisted");
    }

    @Bean
    Binding supplierRatingBinding(Queue supplierReportQueue, TopicExchange supplierExchange) {
        return BindingBuilder.bind(supplierReportQueue).to(supplierExchange).with("supplier.rating-updated");
    }

    @Bean
    Binding supplierPerformanceBinding(Queue supplierReportQueue, TopicExchange supplierExchange) {
        return BindingBuilder.bind(supplierReportQueue).to(supplierExchange).with("supplier.performance-updated");
    }

    @Bean
    MessageConverter supplierMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
