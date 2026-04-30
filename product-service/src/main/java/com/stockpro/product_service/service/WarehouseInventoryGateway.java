package com.stockpro.product_service.service;

import com.stockpro.product_service.dto.WarehouseStockSnapshotDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class WarehouseInventoryGateway implements InventoryAvailabilityGateway {

    private final RestClient.Builder restClientBuilder;

    @Value("${warehouse-service.base-url:http://localhost:8084/stock}")
    private String warehouseServiceBaseUrl;

    @Override
    public Optional<Integer> getAvailableQuantity(Long productId) {
        return fetchStockSnapshots(productId)
                .map(stockLevels -> stockLevels.stream()
                        .map(WarehouseStockSnapshotDTO::getAvailableQuantity)
                        .filter(quantity -> quantity != null && quantity > 0)
                        .reduce(0, Integer::sum));
    }

    @Override
    public boolean hasInventoryUsage(Long productId) {
        return fetchStockSnapshots(productId)
                .map(stockLevels -> stockLevels.stream().anyMatch(stock ->
                        defaultIfNull(stock.getQuantity()) > 0
                                || defaultIfNull(stock.getReservedQuantity()) > 0))
                .orElse(false);
    }

    private Optional<List<WarehouseStockSnapshotDTO>> fetchStockSnapshots(Long productId) {
        try {
            WarehouseStockSnapshotDTO[] response = restClientBuilder
                    .baseUrl(warehouseServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/product/{productId}", productId)
                    .retrieve()
                    .body(WarehouseStockSnapshotDTO[].class);
            if (response == null) {
                return Optional.of(List.of());
            }
            return Optional.of(Arrays.asList(response));
        } catch (RestClientException ex) {
            log.warn("Unable to fetch warehouse stock for product {}: {}",
                    productId, ex.getMessage());
            return Optional.empty();
        }
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
