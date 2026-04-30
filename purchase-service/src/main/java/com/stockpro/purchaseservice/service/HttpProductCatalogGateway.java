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

    @Value("${product-service.base-url:http://localhost:8082/products}")
    private String productServiceBaseUrl;

    @Override
    public StockProductThresholdDTO getProductThresholds(Long productId) {
        try {
            StockProductThresholdDTO response = restClientBuilder.baseUrl(productServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/{productId}", productId)
                    .retrieve()
                    .body(StockProductThresholdDTO.class);
            if (response == null) {
                StockProductThresholdDTO thresholds = new StockProductThresholdDTO();
                thresholds.setProductId(productId);
                return thresholds;
            }
            return response;
        } catch (RestClientResponseException ex) {
            log.warn("Product threshold lookup failed for product {}: {}", productId, ex.getMessage());
            StockProductThresholdDTO thresholds = new StockProductThresholdDTO();
            thresholds.setProductId(productId);
            return thresholds;
        } catch (RestClientException ex) {
            log.warn("Product-service unavailable for product {} threshold lookup: {}",
                    productId, ex.getMessage());
            StockProductThresholdDTO thresholds = new StockProductThresholdDTO();
            thresholds.setProductId(productId);
            return thresholds;
        }
    }
}
