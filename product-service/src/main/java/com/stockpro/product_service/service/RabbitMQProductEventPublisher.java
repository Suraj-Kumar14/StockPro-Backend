package com.stockpro.product_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProductEventPublisher implements ProductEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.product.exchange}")
    private String exchange;

    @Value("${stockpro.rabbitmq.product.routing.created}")
    private String createdRoutingKey;

    @Value("${stockpro.rabbitmq.product.routing.updated}")
    private String updatedRoutingKey;

    @Value("${stockpro.rabbitmq.product.routing.activated}")
    private String activatedRoutingKey;

    @Value("${stockpro.rabbitmq.product.routing.deactivated}")
    private String deactivatedRoutingKey;

    @Value("${stockpro.rabbitmq.product.routing.reorderChanged}")
    private String reorderChangedRoutingKey;

    @Value("${stockpro.rabbitmq.product.routing.deleted}")
    private String deletedRoutingKey;

    @Override
    public void publishProductCreated(ProductLifecycleEvent event) {
        publish(createdRoutingKey, event, "product.created");
    }

    @Override
    public void publishProductUpdated(ProductLifecycleEvent event) {
        publish(updatedRoutingKey, event, "product.updated");
    }

    @Override
    public void publishProductActivated(ProductLifecycleEvent event) {
        publish(activatedRoutingKey, event, "product.activated");
    }

    @Override
    public void publishProductDeactivated(ProductLifecycleEvent event) {
        publish(deactivatedRoutingKey, event, "product.deactivated");
    }

    @Override
    public void publishProductReorderConfigChanged(ProductLifecycleEvent event) {
        publish(reorderChangedRoutingKey, event, "product.reorder-config.changed");
    }

    @Override
    public void publishProductDeleted(ProductLifecycleEvent event) {
        publish(deletedRoutingKey, event, "product.deleted");
    }

    private void publish(String routingKey, ProductLifecycleEvent event, String eventLabel) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("RabbitMQ product event published successfully eventType={} productId={} routingKey={}",
                    eventLabel, event.productId(), routingKey);
        } catch (Exception ex) {
            log.error("RabbitMQ product event publish failed eventType={} productId={} routingKey={} error={}",
                    eventLabel, event.productId(), routingKey, ex.getMessage(), ex);
        }
    }
}
