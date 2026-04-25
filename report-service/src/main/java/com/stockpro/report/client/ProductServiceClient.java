package com.stockpro.report.client;

import com.stockpro.report.config.FeignAuthForwardingConfig;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "${product.service.name:PRODUCT-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "${product.service.path:/products}")
public interface ProductServiceClient {

    @GetMapping("/{productId}")
    ProductClientResponse getProductById(@PathVariable Long productId);

    @GetMapping("/all")
    List<ProductClientResponse> getAllProducts();
}
