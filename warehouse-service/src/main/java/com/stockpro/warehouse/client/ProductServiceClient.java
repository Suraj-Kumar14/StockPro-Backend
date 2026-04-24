package com.stockpro.warehouse.client;

import com.stockpro.warehouse.config.FeignAuthForwardingConfig;
import com.stockpro.warehouse.dto.response.ProductSummaryResponse;
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
    ProductSummaryResponse getProductById(@PathVariable Long productId);

    @GetMapping("/all")
    List<ProductSummaryResponse> getAllProducts();
}
