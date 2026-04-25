package com.stockpro.report.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange reportExchange(ReportRabbitMqProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue snapshotCompletedQueue(ReportRabbitMqProperties properties) {
        return new Queue(properties.getQueues().getSnapshotCompleted(), true);
    }

    @Bean
    public Queue generationRequestedQueue(ReportRabbitMqProperties properties) {
        return new Queue(properties.getQueues().getGenerationRequested(), true);
    }

    @Bean
    public Queue generationCompletedQueue(ReportRabbitMqProperties properties) {
        return new Queue(properties.getQueues().getGenerationCompleted(), true);
    }

    @Bean
    public Binding snapshotCompletedBinding(Queue snapshotCompletedQueue,
            DirectExchange reportExchange,
            ReportRabbitMqProperties properties) {
        return BindingBuilder.bind(snapshotCompletedQueue)
                .to(reportExchange)
                .with(properties.getRoutingKeys().getSnapshotCompleted());
    }

    @Bean
    public Binding generationRequestedBinding(Queue generationRequestedQueue,
            DirectExchange reportExchange,
            ReportRabbitMqProperties properties) {
        return BindingBuilder.bind(generationRequestedQueue)
                .to(reportExchange)
                .with(properties.getRoutingKeys().getGenerationRequested());
    }

    @Bean
    public Binding generationCompletedBinding(Queue generationCompletedQueue,
            DirectExchange reportExchange,
            ReportRabbitMqProperties properties) {
        return BindingBuilder.bind(generationCompletedQueue)
                .to(reportExchange)
                .with(properties.getRoutingKeys().getGenerationCompleted());
    }
}
