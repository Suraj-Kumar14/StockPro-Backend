package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
@RequiredArgsConstructor
public class HttpProductCatalogGateway implements ProductCatalogGateway {

    private final RestClient.Builder restClientBuilder;
    private final DownstreamAuthSupport downstreamAuthSupport;

    @Value("${product-service.base-url:http://localhost:8080/api/v1/products}")
    private String productServiceBaseUrl;

    @Override
    public StockProductThresholdDTO getProductThresholds(Long productId) {
        return fetchProduct(productId);
    }

    @Override
    public StockProductThresholdDTO getProductDetails(Long productId) {
        return fetchProduct(productId);
    }

    private StockProductThresholdDTO fetchProduct(Long productId) {
        try {
            StockProductThresholdDTO response = restClientBuilder.baseUrl(productServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/{productId}", productId)
                    .headers(downstreamAuthSupport::apply)
                    .retrieve()
                    .body(StockProductThresholdDTO.class);
            if (response == null) {
                throw new IllegalArgumentException("Product not found with ID: " + productId);
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new IllegalStateException("Product-service authorization failed", ex);
            }
            if (ex.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException("Product not found with ID: " + productId);
            }
            throw new IllegalStateException("Product-service unavailable", ex);
        } catch (RestClientException ex) {
            log.warn("Product-service unavailable for product {} lookup: {}",
                    productId, ex.getMessage());
            throw new IllegalStateException("Product-service unavailable", ex);
        }
    }
}
