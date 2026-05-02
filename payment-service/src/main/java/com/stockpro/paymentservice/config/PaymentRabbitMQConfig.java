package com.stockpro.paymentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitMQConfig {

    @Bean
    public TopicExchange paymentExchange(@Value("${stockpro.rabbitmq.payment.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue paymentEventsQueue(@Value("${stockpro.rabbitmq.payment.events-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue paymentReportQueue(@Value("${stockpro.rabbitmq.payment.report-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue paymentAlertQueue(@Value("${stockpro.rabbitmq.payment.alert-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue paymentPurchaseQueue(@Value("${stockpro.rabbitmq.payment.purchase-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding paymentEventsBinding(@Qualifier("paymentEventsQueue") Queue paymentEventsQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.*");
    }

    @Bean
    public Binding paymentReportBinding(@Qualifier("paymentReportQueue") Queue paymentReportQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentReportQueue).to(paymentExchange).with("payment.*");
    }

    @Bean
    public Binding paymentSubmittedAlertBinding(@Qualifier("paymentAlertQueue") Queue paymentAlertQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentAlertQueue).to(paymentExchange).with("payment.submitted");
    }

    @Bean
    public Binding paymentApprovedAlertBinding(@Qualifier("paymentAlertQueue") Queue paymentAlertQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentAlertQueue).to(paymentExchange).with("payment.approved");
    }

    @Bean
    public Binding paymentRejectedAlertBinding(@Qualifier("paymentAlertQueue") Queue paymentAlertQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentAlertQueue).to(paymentExchange).with("payment.rejected");
    }

    @Bean
    public Binding paymentCancelledAlertBinding(@Qualifier("paymentAlertQueue") Queue paymentAlertQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentAlertQueue).to(paymentExchange).with("payment.cancelled");
    }

    @Bean
    public Binding paymentPaidPurchaseBinding(@Qualifier("paymentPurchaseQueue") Queue paymentPurchaseQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentPurchaseQueue).to(paymentExchange).with("payment.paid");
    }

    @Bean
    public Binding paymentPartialPurchaseBinding(@Qualifier("paymentPurchaseQueue") Queue paymentPurchaseQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentPurchaseQueue).to(paymentExchange).with("payment.partially-paid");
    }

    @Bean
    public Binding paymentReversedPurchaseBinding(@Qualifier("paymentPurchaseQueue") Queue paymentPurchaseQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentPurchaseQueue).to(paymentExchange).with("payment.reversed");
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
