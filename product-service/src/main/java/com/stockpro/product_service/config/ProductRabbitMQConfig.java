package com.stockpro.product_service.config;

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
public class ProductRabbitMQConfig {

    @Bean
    public TopicExchange productExchange(
            @Value("${stockpro.rabbitmq.product.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue productCreatedQueue() {
        return new Queue("stockpro.product.created.queue", true);
    }

    @Bean
    public Queue productUpdatedQueue() {
        return new Queue("stockpro.product.updated.queue", true);
    }

    @Bean
    public Queue productStatusQueue() {
        return new Queue("stockpro.product.status.queue", true);
    }

    @Bean
    public Queue productReorderQueue() {
        return new Queue("stockpro.product.reorder.queue", true);
    }

    @Bean
    public Binding productCreatedBinding(
            TopicExchange productExchange,
            Queue productCreatedQueue,
            @Value("${stockpro.rabbitmq.product.routing.created}") String routingKey) {
        return BindingBuilder.bind(productCreatedQueue).to(productExchange).with(routingKey);
    }

    @Bean
    public Binding productUpdatedBinding(
            TopicExchange productExchange,
            Queue productUpdatedQueue,
            @Value("${stockpro.rabbitmq.product.routing.updated}") String routingKey) {
        return BindingBuilder.bind(productUpdatedQueue).to(productExchange).with(routingKey);
    }

    @Bean
    public Binding productActivatedBinding(
            TopicExchange productExchange,
            Queue productStatusQueue,
            @Value("${stockpro.rabbitmq.product.routing.activated}") String routingKey) {
        return BindingBuilder.bind(productStatusQueue).to(productExchange).with(routingKey);
    }

    @Bean
    public Binding productDeactivatedBinding(
            TopicExchange productExchange,
            Queue productStatusQueue,
            @Value("${stockpro.rabbitmq.product.routing.deactivated}") String routingKey) {
        return BindingBuilder.bind(productStatusQueue).to(productExchange).with(routingKey);
    }

    @Bean
    public Binding productDeletedBinding(
            TopicExchange productExchange,
            Queue productStatusQueue,
            @Value("${stockpro.rabbitmq.product.routing.deleted}") String routingKey) {
        return BindingBuilder.bind(productStatusQueue).to(productExchange).with(routingKey);
    }

    @Bean
    public Binding productReorderBinding(
            TopicExchange productExchange,
            Queue productReorderQueue,
            @Value("${stockpro.rabbitmq.product.routing.reorderChanged}") String routingKey) {
        return BindingBuilder.bind(productReorderQueue).to(productExchange).with(routingKey);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
