package com.stockpro.warehouseservice.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String LOW_STOCK_QUEUE      = "low-stock-queue";
    public static final String OVERSTOCK_QUEUE      = "overstock-queue";
    public static final String GOODS_RECEIVED_QUEUE = "goods-received-queue";

    public static final String STOCKPRO_EXCHANGE = "stockpro.exchange";

    public static final String LOW_STOCK_ROUTING_KEY      = "stock.low";
    public static final String OVERSTOCK_ROUTING_KEY      = "stock.over";
    public static final String GOODS_RECEIVED_ROUTING_KEY = "stock.goods-received";

    @Bean
    public Queue lowStockQueue() {
        return new Queue(LOW_STOCK_QUEUE, true);
    }

    @Bean
    public Queue overstockQueue() {
        return new Queue(OVERSTOCK_QUEUE, true);
    }

    @Bean
    public Queue goodsReceivedQueue() {
        return new Queue(GOODS_RECEIVED_QUEUE, true);
    }

    @Bean
    public TopicExchange stockproExchange() {
        return new TopicExchange(STOCKPRO_EXCHANGE, true, false);
    }

    @Bean
    public Binding lowStockBinding(Queue lowStockQueue, TopicExchange stockproExchange) {
        return BindingBuilder.bind(lowStockQueue).to(stockproExchange).with(LOW_STOCK_ROUTING_KEY);
    }

    @Bean
    public Binding overstockBinding(Queue overstockQueue, TopicExchange stockproExchange) {
        return BindingBuilder.bind(overstockQueue).to(stockproExchange).with(OVERSTOCK_ROUTING_KEY);
    }

    @Bean
    public Binding goodsReceivedBinding(Queue goodsReceivedQueue, TopicExchange stockproExchange) {
        return BindingBuilder.bind(goodsReceivedQueue).to(stockproExchange).with(GOODS_RECEIVED_ROUTING_KEY);
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