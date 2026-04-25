package com.stockpro.alert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(AlertRabbitMqProperties.class)
public class RabbitMqConfig {

    private final AlertRabbitMqProperties properties;

    public RabbitMqConfig(AlertRabbitMqProperties properties) {
        this.properties = properties;
    }

    @Bean
    public DirectExchange alertExchange() {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public DirectExchange alertDeadLetterExchange() {
        return new DirectExchange(properties.getDlx(), true, false);
    }

    @Bean
    public Queue lowStockQueue() {
        return buildDurableQueue(properties.getQueues().getLowStock());
    }

    @Bean
    public Queue overstockQueue() {
        return buildDurableQueue(properties.getQueues().getOverstock());
    }

    @Bean
    public Queue poPendingQueue() {
        return buildDurableQueue(properties.getQueues().getPoPending());
    }

    @Bean
    public Queue overdueReceiptQueue() {
        return buildDurableQueue(properties.getQueues().getOverdueReceipt());
    }

    @Bean
    public Queue systemAlertQueue() {
        return buildDurableQueue(properties.getQueues().getSystem());
    }

    @Bean
    public Queue emailQueue() {
        return buildDurableQueue(properties.getQueues().getEmail());
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(properties.getDeadQueue()).build();
    }

    @Bean
    public Binding lowStockBinding(Queue lowStockQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(lowStockQueue).to(alertExchange).with(properties.getRoutingKeys().getLowStock());
    }

    @Bean
    public Binding overstockBinding(Queue overstockQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(overstockQueue).to(alertExchange).with(properties.getRoutingKeys().getOverstock());
    }

    @Bean
    public Binding poPendingBinding(Queue poPendingQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(poPendingQueue).to(alertExchange).with(properties.getRoutingKeys().getPoPending());
    }

    @Bean
    public Binding overdueReceiptBinding(Queue overdueReceiptQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(overdueReceiptQueue).to(alertExchange).with(properties.getRoutingKeys().getOverdueReceipt());
    }

    @Bean
    public Binding systemBinding(Queue systemAlertQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(systemAlertQueue).to(alertExchange).with(properties.getRoutingKeys().getSystem());
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(emailQueue).to(alertExchange).with(properties.getRoutingKeys().getEmail());
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange alertDeadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(alertDeadLetterExchange).with(properties.getDeadRoutingKey());
    }

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
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(properties.getRetry().getMaxAttempts())
                .backOffOptions(
                        properties.getRetry().getInitialInterval(),
                        properties.getRetry().getMultiplier(),
                        properties.getRetry().getMaxInterval())
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }

    private Queue buildDurableQueue(String queueName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", properties.getDlx())
                .withArgument("x-dead-letter-routing-key", properties.getDeadRoutingKey())
                .build();
    }
}
