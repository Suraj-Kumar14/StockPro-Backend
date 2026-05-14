package com.stockpro.paymentservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitMQConfig {

    @Bean
    TopicExchange paymentExchange(@Value("${stockpro.rabbitmq.payment.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    TopicExchange alertExchange(@Value("${stockpro.rabbitmq.alert.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    MessageConverter paymentMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
