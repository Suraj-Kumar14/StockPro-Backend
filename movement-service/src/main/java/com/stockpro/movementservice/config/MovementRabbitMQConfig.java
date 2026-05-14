package com.stockpro.movementservice.config;

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
public class MovementRabbitMQConfig {

    @Bean
    TopicExchange movementExchange(@Value("${stockpro.rabbitmq.movement.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean Queue movementEventsQueue() { return new Queue("stockpro.movement.events.queue", true); }
    @Bean Queue movementReportQueue() { return new Queue("stockpro.movement.report.queue", true); }
    @Bean Queue movementAuditQueue() { return new Queue("stockpro.movement.audit.queue", true); }
    @Bean Queue stockMovementQueue(@Value("${stockpro.rabbitmq.stock.movement.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean Binding movementEventsBinding(Queue movementEventsQueue, TopicExchange movementExchange) {
        return BindingBuilder.bind(movementEventsQueue).to(movementExchange).with("movement.*");
    }

    @Bean Binding movementReportBinding(Queue movementReportQueue, TopicExchange movementExchange) {
        return BindingBuilder.bind(movementReportQueue).to(movementExchange).with("movement.*");
    }

    @Bean Binding movementAuditBinding(Queue movementAuditQueue, TopicExchange movementExchange) {
        return BindingBuilder.bind(movementAuditQueue).to(movementExchange).with("movement.*");
    }

    @Bean
    MessageConverter movementMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
