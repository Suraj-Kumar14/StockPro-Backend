package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;
import com.stockpro.purchaseservice.dto.WarehouseLookupResponseDTO;
import com.stockpro.purchaseservice.dto.WarehouseStockUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
@RequiredArgsConstructor
public class HttpWarehouseGateway implements WarehouseGateway {
    private static final String[] WAREHOUSE_SERVICE_PATH_SUFFIXES = {
            "/api/v1/warehouses",
            "/warehouses",
            "/api/v1/stocks",
            "/stock"
    };

    private final RestClient.Builder restClientBuilder;
    private final DownstreamAuthSupport downstreamAuthSupport;

    @Value("${warehouse-service.base-url:http://localhost:8080/api/v1/warehouses}")
    private String warehouseServiceBaseUrl;

    @Override
    public void ensureWarehouseExists(Long warehouseId) {
        getWarehouse(warehouseId);
    }

    @Override
    public WarehouseLookupResponseDTO getWarehouse(Long warehouseId) {
        try {
            WarehouseLookupResponseDTO response = restClientBuilder.baseUrl(resolveServiceRootUrl())
                    .build()
                    .get()
                    .uri("/api/v1/warehouses/{warehouseId}", warehouseId)
                    .headers(downstreamAuthSupport::apply)
                    .retrieve()
                    .body(WarehouseLookupResponseDTO.class);
            if (response == null || response.getWarehouseId() == null
                    || Boolean.FALSE.equals(response.getIsActive())) {
                throw new IllegalArgumentException(
                        "Warehouse not found with ID: " + warehouseId);
            }
            return response;
        } catch (RestClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            if (statusCode.value() == 401 || statusCode.value() == 403) {
                throw new IllegalStateException("Warehouse-service authorization failed", ex);
            }
            if (statusCode.is4xxClientError()) {
                throw new IllegalArgumentException(
                        "Warehouse not found with ID: " + warehouseId);
            }
            throw new IllegalStateException("Warehouse-service unavailable", ex);
        } catch (RestClientException ex) {
            log.error("Warehouse lookup failed for warehouse {}: {}",
                    warehouseId, ex.getMessage());
            throw new IllegalStateException("Warehouse-service unavailable", ex);
        }
    }

    @Override
    public void increaseStock(Long warehouseId, Long productId, Integer quantity,
            StockProductThresholdDTO thresholds) {
        try {
            WarehouseStockUpdateDTO request = WarehouseStockUpdateDTO.builder()
                    .warehouseId(warehouseId)
                    .productId(productId)
                    .quantity(quantity)
                    .reason("Purchase order goods receipt")
                    .notes("Purchase-service synchronous goods receipt")
                    .reorderLevel(thresholds != null ? thresholds.getReorderLevel() : null)
                    .maxStockLevel(thresholds != null ? thresholds.getMaxStockLevel() : null)
                    .build();

            restClientBuilder.baseUrl(resolveServiceRootUrl())
                    .build()
                    .post()
                    .uri("/api/v1/stocks/receive")
                    .headers(downstreamAuthSupport::apply)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Warehouse stock update failed for warehouse {}, product {}: {}",
                    warehouseId, productId, ex.getMessage());
            throw new IllegalStateException("Warehouse stock update failed", ex);
        } catch (RestClientException ex) {
            log.error("Warehouse stock update transport failure for warehouse {}, product {}: {}",
                    warehouseId, productId, ex.getMessage());
            throw new IllegalStateException("Warehouse stock update failed", ex);
        }
    }

    private String resolveServiceRootUrl() {
        String normalizedBaseUrl = trimTrailingSlash(warehouseServiceBaseUrl);
        for (String suffix : WAREHOUSE_SERVICE_PATH_SUFFIXES) {
            if (normalizedBaseUrl.endsWith(suffix)) {
                return normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - suffix.length());
            }
        }
        return normalizedBaseUrl;
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
