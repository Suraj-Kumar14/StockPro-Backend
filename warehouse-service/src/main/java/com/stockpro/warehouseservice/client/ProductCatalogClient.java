package com.stockpro.warehouseservice.client;

import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.exception.ProductLookupException;
import com.stockpro.warehouseservice.service.DownstreamAuthSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductCatalogClient {

    private final RestClient restClient;
    private final DownstreamAuthSupport downstreamAuthSupport;

    public ProductCatalogClient(
            RestClient.Builder restClientBuilder,
            DownstreamAuthSupport downstreamAuthSupport,
            @Value("${product-service.base-url:http://localhost:8080/api/v1/products}") String baseUrl) {

        this.restClient = restClientBuilder
                .baseUrl(resolveProductsBaseUrl(baseUrl))
                .build();

        this.downstreamAuthSupport = downstreamAuthSupport;
    }

    public ProductLookupResponseDTO getProductByBarcode(String barcode) {
        try {
            ProductLookupResponseDTO response = restClient.get()
                    .uri("/barcode/{barcode}", barcode)
                    .headers(downstreamAuthSupport::apply)
                    .retrieve()
                    .body(ProductLookupResponseDTO.class);

            if (response == null) {
                throw new ProductLookupException("Product not found with barcode: " + barcode);
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new ProductLookupException("Product-service authorization failed", true);
            }
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ProductLookupException("Product not found with barcode: " + barcode);
            }
            throw new ProductLookupException("Unable to fetch product details from product-service", true);
        } catch (RestClientException ex) {
            throw new ProductLookupException("Unable to fetch product details from product-service", true);
        }
    }

    public ProductLookupResponseDTO getProductById(Long productId) {
        try {
            ProductLookupResponseDTO response = restClient.get()
                    .uri("/{productId}", productId)
                    .headers(downstreamAuthSupport::apply)
                    .retrieve()
                    .body(ProductLookupResponseDTO.class);

            if (response == null) {
                throw new ProductLookupException("Product not found with ID: " + productId);
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new ProductLookupException("Product-service authorization failed", true);
            }
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ProductLookupException("Product not found with ID: " + productId);
            }
            throw new ProductLookupException("Unable to fetch product details from product-service", true);
        } catch (RestClientException ex) {
            throw new ProductLookupException("Unable to fetch product details from product-service", true);
        }
    }

    private String resolveProductsBaseUrl(String configuredBaseUrl) {
        String normalizedBaseUrl = trimTrailingSlash(configuredBaseUrl);

        if (normalizedBaseUrl.endsWith("/api/v1/products")
                || normalizedBaseUrl.endsWith("/products")) {
            return normalizedBaseUrl;
        }

        return normalizedBaseUrl + "/api/v1/products";
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}
