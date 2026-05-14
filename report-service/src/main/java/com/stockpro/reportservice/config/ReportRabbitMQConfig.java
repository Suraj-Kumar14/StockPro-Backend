package com.stockpro.reportservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ReportRabbitMQConfig {

    @Bean
    TopicExchange reportExchange(@Value("${stockpro.rabbitmq.report.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue reportEventsQueue(@Value("${stockpro.rabbitmq.report.events-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Queue reportAuditQueue(@Value("${stockpro.rabbitmq.report.audit-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Queue stockReportQueue(@Value("${stockpro.rabbitmq.report.stock-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Queue purchaseReportQueue(@Value("${stockpro.rabbitmq.report.purchase-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Queue paymentReportQueue(@Value("${stockpro.rabbitmq.report.payment-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Queue supplierReportQueue(@Value("${stockpro.rabbitmq.report.supplier-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Queue movementReportQueue(@Value("${stockpro.rabbitmq.report.movement-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Queue alertReportQueue(@Value("${stockpro.rabbitmq.report.alert-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding generatedBinding(@Qualifier("reportEventsQueue") Queue reportEventsQueue, TopicExchange reportExchange,
                             @Value("${stockpro.rabbitmq.report.generated-routing-key}") String routingKey) {
        return BindingBuilder.bind(reportEventsQueue).to(reportExchange).with(routingKey);
    }

    @Bean
    Binding snapshotBinding(@Qualifier("reportAuditQueue") Queue reportAuditQueue, TopicExchange reportExchange,
                            @Value("${stockpro.rabbitmq.report.snapshot-routing-key}") String routingKey) {
        return BindingBuilder.bind(reportAuditQueue).to(reportExchange).with(routingKey);
    }

    @Bean
    Binding exportCompletedBinding(@Qualifier("reportAuditQueue") Queue reportAuditQueue, TopicExchange reportExchange,
                                   @Value("${stockpro.rabbitmq.report.export-completed-routing-key}") String routingKey) {
        return BindingBuilder.bind(reportAuditQueue).to(reportExchange).with(routingKey);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
