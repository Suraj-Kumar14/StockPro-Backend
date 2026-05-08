package com.stockpro.warehouseservice.client;

import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.exception.ProductLookupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductCatalogClient {
    private static final String[] PRODUCT_SERVICE_PATH_SUFFIXES = {
            "/api/v1/products",
            "/products"
    };

    private final RestClient restClient;

    public ProductCatalogClient(
            RestClient.Builder restClientBuilder,
            @Value("${product-service.base-url:http://localhost:8080/api/v1/products}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(resolveProductsBaseUrl(baseUrl)).build();
    }

    public ProductLookupResponseDTO getProductByBarcode(String barcode) {
        try {
            ProductLookupResponseDTO response = restClient.get()
                    .uri("/barcode/{barcode}", barcode)
                    .retrieve()
                    .body(ProductLookupResponseDTO.class);

            if (response == null) {
                throw new ProductLookupException(
                        "Product not found with barcode: " + barcode);
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ProductLookupException(
                        "Product not found with barcode: " + barcode);
            }
            throw new ProductLookupException(
                    "Unable to fetch product details from product-service", true);
        } catch (RestClientException ex) {
            throw new ProductLookupException(
                    "Unable to fetch product details from product-service", true);
        }
    }

    public ProductLookupResponseDTO getProductById(Long productId) {
        try {
            ProductLookupResponseDTO response = restClient.get()
                    .uri("/{productId}", productId)
                    .retrieve()
                    .body(ProductLookupResponseDTO.class);

            if (response == null) {
                throw new ProductLookupException(
                        "Product not found with ID: " + productId);
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ProductLookupException(
                        "Product not found with ID: " + productId);
            }
            throw new ProductLookupException(
                    "Unable to fetch product details from product-service", true);
        } catch (RestClientException ex) {
            throw new ProductLookupException(
                    "Unable to fetch product details from product-service", true);
        }
    }

    private String resolveProductsBaseUrl(String configuredBaseUrl) {
        String normalizedBaseUrl = trimTrailingSlash(configuredBaseUrl);
        for (String suffix : PRODUCT_SERVICE_PATH_SUFFIXES) {
            if (normalizedBaseUrl.endsWith(suffix)) {
                return normalizedBaseUrl;
            }
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
