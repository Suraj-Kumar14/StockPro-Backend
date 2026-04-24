package com.stockpro.movement.client;

import com.stockpro.movement.config.FeignAuthForwardingConfig;
import com.stockpro.movement.dto.response.ProductSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "${product.service.name:PRODUCT-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "${product.service.path:/products}")
public interface ProductServiceClient {

    @GetMapping("/{productId}")
    ProductSummaryResponse getProductById(@PathVariable Long productId);
}
