package com.stockpro.purchaseservice.rabbitmq;

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
public class RabbitMQConfig {

    public static final String EXCHANGE = "stockpro.purchase.exchange";
    public static final String PO_APPROVED_KEY = "purchase.approved";
    public static final String PO_PENDING_KEY = "purchase.pending-approval";
    public static final String PO_OVERDUE_KEY = "purchase.overdue";

    @Bean
    public Queue poApprovedQueue(
            @Value("${stockpro.rabbitmq.purchase.approved.queue:stockpro.purchase.approved.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue poPendingQueue(
            @Value("${stockpro.rabbitmq.purchase.pending.queue:stockpro.purchase.pending-approval.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue poOverdueQueue(
            @Value("${stockpro.rabbitmq.purchase.overdue.queue:stockpro.purchase.overdue.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public TopicExchange stockproExchange(@Value("${stockpro.rabbitmq.purchase.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding poApprovedBinding(
            Queue poApprovedQueue,
            TopicExchange stockproExchange,
            @Value("${stockpro.rabbitmq.purchase.routing.approved}") String routingKey) {
        return BindingBuilder
                .bind(poApprovedQueue)
                .to(stockproExchange)
                .with(routingKey);
    }

    @Bean
    public Binding poPendingBinding(
            Queue poPendingQueue,
            TopicExchange stockproExchange,
            @Value("${stockpro.rabbitmq.purchase.routing.pendingApproval}") String routingKey) {
        return BindingBuilder
                .bind(poPendingQueue)
                .to(stockproExchange)
                .with(routingKey);
    }

    @Bean
    public Binding poOverdueBinding(
            Queue poOverdueQueue,
            TopicExchange stockproExchange,
            @Value("${stockpro.rabbitmq.purchase.routing.overdue}") String routingKey) {
        return BindingBuilder
                .bind(poOverdueQueue)
                .to(stockproExchange)
                .with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
