package com.stockpro.product_service.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnMissingBean(ProductEventPublisher.class)
@Slf4j
public class NoOpProductEventPublisher implements ProductEventPublisher {

    @Override
    public void publishProductCreated(ProductLifecycleEvent event) {
        log.debug("ProductCreatedEvent placeholder: {}", event);
    }

    @Override
    public void publishProductUpdated(ProductLifecycleEvent event) {
        log.debug("ProductUpdatedEvent placeholder: {}", event);
    }

    @Override
    public void publishProductActivated(ProductLifecycleEvent event) {
        log.debug("ProductActivatedEvent placeholder: {}", event);
    }

    @Override
    public void publishProductDeactivated(ProductLifecycleEvent event) {
        log.debug("ProductDeactivatedEvent placeholder: {}", event);
    }

    @Override
    public void publishProductReorderConfigChanged(ProductLifecycleEvent event) {
        log.debug("ProductReorderConfigChangedEvent placeholder: {}", event);
    }

    @Override
    public void publishProductDeleted(ProductLifecycleEvent event) {
        log.debug("ProductDeletedEvent placeholder: {}", event);
    }
}
