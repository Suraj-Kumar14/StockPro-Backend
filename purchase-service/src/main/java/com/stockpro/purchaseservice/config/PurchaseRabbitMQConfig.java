package com.stockpro.purchaseservice.config;

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
public class PurchaseRabbitMQConfig {

    @Bean
    TopicExchange purchaseExchange(@Value("${stockpro.rabbitmq.purchase.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean Queue purchaseEventsQueue() { return new Queue("stockpro.purchase.events.queue", true); }
    @Bean Queue purchaseAlertQueue() { return new Queue("stockpro.purchase.alert.queue", true); }
    @Bean Queue purchaseReportQueue() { return new Queue("stockpro.purchase.report.queue", true); }
    @Bean Queue purchaseStockQueue() { return new Queue("stockpro.purchase.stock.queue", true); }
    @Bean Queue purchasePaymentQueue() { return new Queue("stockpro.purchase.payment.queue", true); }

    @Bean Binding purchaseEventsBinding(Queue purchaseEventsQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseEventsQueue).to(purchaseExchange).with("purchase.*");
    }

    @Bean Binding purchaseSubmittedAlertBinding(Queue purchaseAlertQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseAlertQueue).to(purchaseExchange).with("purchase.submitted");
    }

    @Bean Binding purchasePendingAlertBinding(Queue purchaseAlertQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseAlertQueue).to(purchaseExchange).with("purchase.pending-approval");
    }

    @Bean Binding purchaseOverdueAlertBinding(Queue purchaseAlertQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseAlertQueue).to(purchaseExchange).with("purchase.overdue");
    }

    @Bean Binding purchaseApprovedReportBinding(Queue purchaseReportQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseReportQueue).to(purchaseExchange).with("purchase.approved");
    }

    @Bean Binding purchasePartialStockBinding(Queue purchaseStockQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseStockQueue).to(purchaseExchange).with("purchase.partially-received");
    }

    @Bean Binding purchaseFullStockBinding(Queue purchaseStockQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseStockQueue).to(purchaseExchange).with("purchase.fully-received");
    }

    @Bean Binding purchasePartialReportBinding(Queue purchaseReportQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseReportQueue).to(purchaseExchange).with("purchase.partially-received");
    }

    @Bean Binding purchaseFullReportBinding(Queue purchaseReportQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchaseReportQueue).to(purchaseExchange).with("purchase.fully-received");
    }

    @Bean Binding purchasePaymentBinding(Queue purchasePaymentQueue, TopicExchange purchaseExchange) {
        return BindingBuilder.bind(purchasePaymentQueue).to(purchaseExchange).with("purchase.fully-received");
    }

    @Bean
    MessageConverter purchaseMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
