package com.stockpro.web.client;

import com.stockpro.web.config.FeignSupportConfig;
import com.stockpro.web.dto.request.ProductFormRequest;
import com.stockpro.web.dto.request.ProductSearchRequest;
import com.stockpro.web.dto.response.ApiPageResponse;
import com.stockpro.web.dto.response.LowStockProductResponse;
import com.stockpro.web.dto.response.ProductResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "productServiceClient",
        url = "${api.gateway.base-url}",
        configuration = FeignSupportConfig.class)
public interface ProductServiceClient {

    @GetMapping("/products")
    ApiPageResponse<ProductResponse> getAllProducts(@RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortBy,
            @RequestParam String sortDir);

    @PostMapping("/products/search")
    ApiPageResponse<ProductResponse> searchProducts(@RequestBody ProductSearchRequest request);

    @GetMapping("/products/{productId}")
    ProductResponse getProductById(@PathVariable("productId") Long productId);

    @GetMapping("/products/barcode/{barcode}")
    ProductResponse getProductByBarcode(@PathVariable("barcode") String barcode);

    @PostMapping("/products")
    ProductResponse createProduct(@RequestBody ProductFormRequest request);

    @PutMapping("/products/{productId}")
    ProductResponse updateProduct(@PathVariable("productId") Long productId,
            @RequestBody ProductFormRequest request);

    @PutMapping("/products/{productId}/deactivate")
    ProductResponse deactivateProduct(@PathVariable("productId") Long productId);

    @GetMapping("/products/low-stock")
    List<LowStockProductResponse> getLowStockProducts();
}
