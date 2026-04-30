package com.stockpro.reportservice.service;

import com.stockpro.reportservice.exception.ReportGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportDataGateway {

    private static final ParameterizedTypeReference<List<WarehouseView>> WAREHOUSE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<StockLevelView>> STOCK_LEVEL_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ProductView>> PRODUCT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<PurchaseOrderView>> PURCHASE_ORDER_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<StockMovementView>> MOVEMENT_LIST =
            new ParameterizedTypeReference<>() {};

    private final WebClient.Builder webClientBuilder;

    @Value("${services.warehouse-url}")
    private String warehouseUrl;

    @Value("${services.product-url}")
    private String productUrl;

    @Value("${services.movement-url}")
    private String movementUrl;

    @Value("${services.purchase-url}")
    private String purchaseUrl;

    public List<WarehouseView> fetchWarehouses() {
        return getList(warehouseUrl + "/warehouses", WAREHOUSE_LIST, "warehouses");
    }

    public List<StockLevelView> fetchStockByWarehouse(Long warehouseId) {
        return getList(warehouseUrl + "/stock/warehouse/" + warehouseId, STOCK_LEVEL_LIST,
                "stock for warehouse " + warehouseId);
    }

    public List<StockLevelView> fetchLowStockItems(int threshold) {
        return getList(warehouseUrl + "/stock/low-stock?threshold=" + threshold,
                STOCK_LEVEL_LIST, "low stock items");
    }

    public List<ProductView> fetchProducts() {
        return getList(productUrl + "/products", PRODUCT_LIST, "products");
    }

    public List<PurchaseOrderView> fetchPurchaseOrders(LocalDate startDate, LocalDate endDate) {
        return getList(purchaseUrl + "/purchase-orders/date-range?startDate=" + startDate + "&endDate=" + endDate,
                PURCHASE_ORDER_LIST, "purchase orders");
    }

    public List<StockMovementView> fetchMovements(LocalDate startDate, LocalDate endDate) {
        String start = startDate.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME);
        String end = endDate.plusDays(1).atStartOfDay().minusNanos(1)
                .format(DateTimeFormatter.ISO_DATE_TIME);
        return getList(movementUrl + "/movements/date-range?start=" + start + "&end=" + end,
                MOVEMENT_LIST, "stock movements");
    }

    private <T> List<T> getList(String uri, ParameterizedTypeReference<List<T>> type, String label) {
        try {
            List<T> result = webClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(type)
                    .block();
            return result == null ? List.of() : result;
        } catch (Exception ex) {
            log.error("Failed to fetch {} from {}", label, uri, ex);
            throw new ReportGenerationException("Failed to fetch " + label, ex);
        }
    }

    public record WarehouseView(Long warehouseId, String name, Boolean isActive) {
    }

    public record StockLevelView(
            Long stockId,
            Long warehouseId,
            Long productId,
            Integer quantity,
            Integer reservedQuantity,
            Integer availableQuantity
    ) {
    }

    public record ProductView(
            Long productId,
            String name,
            java.math.BigDecimal costPrice,
            Integer reorderLevel,
            Integer maxStockLevel,
            Boolean isActive
    ) {
    }

    public record PurchaseOrderView(
            Long poId,
            Long supplierId,
            Long warehouseId,
            String status,
            java.math.BigDecimal totalAmount,
            LocalDate orderDate,
            LocalDate expectedDate,
            LocalDate receivedDate
    ) {
    }

    public record StockMovementView(
            Long movementId,
            Long productId,
            Long warehouseId,
            String movementType,
            Integer quantity,
            Long referenceId,
            String referenceType,
            java.math.BigDecimal unitCost,
            LocalDateTime movementDate,
            Integer balanceAfter
    ) {
    }
}
