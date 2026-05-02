package com.stockpro.warehouseservice.rabbitmq;

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

    public static final String STOCKPRO_EXCHANGE = "stockpro.warehouse.exchange";
    public static final String LOW_STOCK_ROUTING_KEY = "stock.low";
    public static final String OVERSTOCK_ROUTING_KEY = "stock.overstock";
    public static final String GOODS_RECEIVED_ROUTING_KEY = "stock.received";

    @Bean
    public Queue lowStockQueue(
            @Value("${stockpro.rabbitmq.warehouse.low-stock.queue:stockpro.warehouse.low-stock.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue overstockQueue(
            @Value("${stockpro.rabbitmq.warehouse.overstock.queue:stockpro.warehouse.overstock.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Queue goodsReceivedQueue(
            @Value("${stockpro.rabbitmq.warehouse.stock-received.queue:stockpro.warehouse.stock-received.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public TopicExchange stockproExchange(@Value("${stockpro.rabbitmq.warehouse.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding lowStockBinding(
            Queue lowStockQueue,
            TopicExchange stockproExchange,
            @Value("${stockpro.rabbitmq.warehouse.routing.stock-low}") String routingKey) {
        return BindingBuilder.bind(lowStockQueue).to(stockproExchange).with(routingKey);
    }

    @Bean
    public Binding overstockBinding(
            Queue overstockQueue,
            TopicExchange stockproExchange,
            @Value("${stockpro.rabbitmq.warehouse.routing.stock-overstock}") String routingKey) {
        return BindingBuilder.bind(overstockQueue).to(stockproExchange).with(routingKey);
    }

    @Bean
    public Binding goodsReceivedBinding(
            Queue goodsReceivedQueue,
            TopicExchange stockproExchange,
            @Value("${stockpro.rabbitmq.warehouse.routing.stock-received}") String routingKey) {
        return BindingBuilder.bind(goodsReceivedQueue).to(stockproExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
