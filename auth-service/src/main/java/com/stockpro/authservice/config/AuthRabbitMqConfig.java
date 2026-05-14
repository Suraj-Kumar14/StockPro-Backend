package com.stockpro.authservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AuthRabbitMqConfig {

    @Bean
    TopicExchange authExchange(@Value("${stockpro.rabbitmq.auth.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue otpQueue(@Value("${stockpro.rabbitmq.auth.otp.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Binding otpBinding(
            Queue otpQueue,
            TopicExchange authExchange,
            @Value("${stockpro.rabbitmq.auth.otp.routing-key}") String routingKey) {
        return BindingBuilder.bind(otpQueue).to(authExchange).with(routingKey);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
