package com.stockpro.purchase.client;

import com.stockpro.purchase.config.FeignAuthForwardingConfig;
import com.stockpro.purchase.dto.ProductSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "${product.service.name:PRODUCT-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "/products")
public interface ProductClient {

    @GetMapping("/{productId}")
    ProductSummaryResponse getProductById(@PathVariable Long productId);
}
