package com.stockpro.product_service.service;

public interface ProductEventPublisher {

    void publishProductCreated(ProductLifecycleEvent event);

    void publishProductUpdated(ProductLifecycleEvent event);

    void publishProductActivated(ProductLifecycleEvent event);

    void publishProductDeactivated(ProductLifecycleEvent event);

    void publishProductReorderConfigChanged(ProductLifecycleEvent event);

    void publishProductDeleted(ProductLifecycleEvent event);
}
