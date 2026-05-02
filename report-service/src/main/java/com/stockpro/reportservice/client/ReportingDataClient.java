package com.stockpro.reportservice.client;

import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.exception.ReportGenerationException;
import com.stockpro.reportservice.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingDataClient {

    private static final int FETCH_SIZE = 500;
    private static final ParameterizedTypeReference<PageResponse<ProductRecord>> PRODUCT_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<WarehouseRecord>> WAREHOUSE_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<StockRecord>> STOCK_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<MovementRecord>> MOVEMENT_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<PurchaseOrderRecord>> PURCHASE_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<SupplierRecord>> SUPPLIER_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<PaymentRecord>> PAYMENT_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<AlertRecord>> ALERT_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<StockRecord>> STOCK_LIST = new ParameterizedTypeReference<>() {};

    private final WebClient.Builder webClientBuilder;

    @Value("${services.product-url}")
    private String productUrl;

    @Value("${services.warehouse-url}")
    private String warehouseUrl;

    @Value("${services.movement-url}")
    private String movementUrl;

    @Value("${services.purchase-url}")
    private String purchaseUrl;

    @Value("${services.supplier-url}")
    private String supplierUrl;

    @Value("${services.payment-url}")
    private String paymentUrl;

    @Value("${services.alert-url}")
    private String alertUrl;

    @Value("${report.internal-service-token:}")
    private String internalServiceToken;

    public List<ProductRecord> getProducts() {
        return getPage(productUrl, "/api/v1/products", "PRODUCT-SERVICE", "getProducts", PRODUCT_PAGE, builder -> builder
                .queryParam("page", 0)
                .queryParam("size", FETCH_SIZE)
                .queryParam("sortBy", "name")
                .queryParam("sortDir", "asc")).content();
    }

    public List<WarehouseRecord> getWarehouses() {
        return getPage(warehouseUrl, "/api/v1/warehouses", "WAREHOUSE-SERVICE", "getWarehouses", WAREHOUSE_PAGE, builder -> builder
                .queryParam("page", 0)
                .queryParam("size", FETCH_SIZE)
                .queryParam("sortBy", "name")
                .queryParam("sortDir", "asc")).content();
    }

    public List<StockRecord> getStocks(ReportFilterRequest filter) {
        return getPage(warehouseUrl, "/api/v1/stocks", "WAREHOUSE-SERVICE", "getStocks", STOCK_PAGE, builder -> {
            if (filter.getWarehouseId() != null) {
                builder.queryParam("warehouseId", filter.getWarehouseId());
            }
            if (filter.getProductId() != null) {
                builder.queryParam("productId", filter.getProductId());
            }
            builder.queryParam("page", 0).queryParam("size", FETCH_SIZE);
        }).content();
    }

    public List<StockRecord> getLowStockItems() {
        return getList(warehouseUrl, "/api/v1/stocks/low-stock", "WAREHOUSE-SERVICE", "getLowStockItems", STOCK_LIST, null);
    }

    public List<StockRecord> getOverstockItems() {
        return getList(warehouseUrl, "/api/v1/stocks/overstock", "WAREHOUSE-SERVICE", "getOverstockItems", STOCK_LIST, null);
    }

    public PageResponse<MovementRecord> searchMovements(ReportFilterRequest filter) {
        return getPage(movementUrl, "/api/v1/movements/search", "MOVEMENT-SERVICE", "searchMovements", MOVEMENT_PAGE, builder -> {
            applyFilterRange(filter, builder);
            if (filter.getWarehouseId() != null) {
                builder.queryParam("warehouseId", filter.getWarehouseId());
            }
            if (filter.getProductId() != null) {
                builder.queryParam("productId", filter.getProductId());
            }
            if (StringUtils.hasText(filter.getMovementType())) {
                builder.queryParam("movementType", filter.getMovementType());
            }
            builder.queryParam("page", filter.getPage())
                    .queryParam("size", Math.max(filter.getSize(), FETCH_SIZE))
                    .queryParam("sortBy", StringUtils.hasText(filter.getSortBy()) ? filter.getSortBy() : "movementDate")
                    .queryParam("sortDir", StringUtils.hasText(filter.getSortDir()) ? filter.getSortDir() : "desc");
        });
    }

    public List<PurchaseOrderRecord> searchPurchaseOrders(ReportFilterRequest filter) {
        return getPage(purchaseUrl, "/api/v1/purchase-orders/search", "PURCHASE-SERVICE", "searchPurchaseOrders", PURCHASE_PAGE, builder -> {
            if (filter.getSupplierId() != null) {
                builder.queryParam("supplierId", filter.getSupplierId());
            }
            if (filter.getWarehouseId() != null) {
                builder.queryParam("warehouseId", filter.getWarehouseId());
            }
            if (StringUtils.hasText(filter.getPoStatus())) {
                builder.queryParam("status", filter.getPoStatus());
            }
            if (filter.getFromDate() != null) {
                builder.queryParam("fromDate", filter.getFromDate());
            }
            if (filter.getToDate() != null) {
                builder.queryParam("toDate", filter.getToDate());
            }
            builder.queryParam("page", 0)
                    .queryParam("size", FETCH_SIZE)
                    .queryParam("sortBy", "createdAt")
                    .queryParam("sortDir", "desc");
        }).content();
    }

    public List<SupplierRecord> getSuppliers() {
        return getPage(supplierUrl, "/api/v1/suppliers", "SUPPLIER-SERVICE", "getSuppliers", SUPPLIER_PAGE, builder -> builder
                .queryParam("page", 0)
                .queryParam("size", FETCH_SIZE)
                .queryParam("sortBy", "name")
                .queryParam("sortDir", "asc")).content();
    }

    public List<PaymentRecord> searchPayments(ReportFilterRequest filter) {
        return getPage(paymentUrl, "/api/v1/payments/search", "PAYMENT-SERVICE", "searchPayments", PAYMENT_PAGE, builder -> {
            if (filter.getSupplierId() != null) {
                builder.queryParam("supplierId", filter.getSupplierId());
            }
            if (StringUtils.hasText(filter.getPaymentStatus())) {
                builder.queryParam("status", filter.getPaymentStatus());
            }
            if (filter.getFromDate() != null) {
                builder.queryParam("fromDate", filter.getFromDate());
            }
            if (filter.getToDate() != null) {
                builder.queryParam("toDate", filter.getToDate());
            }
            builder.queryParam("page", 0)
                    .queryParam("size", FETCH_SIZE)
                    .queryParam("sortBy", "createdAt")
                    .queryParam("sortDir", "desc");
        }).content();
    }

    public PaymentSummaryRecord getPaymentSummary() {
        return getObject(paymentUrl, "/api/v1/payments/summary", "PAYMENT-SERVICE", "getPaymentSummary", PaymentSummaryRecord.class, null);
    }

    public AlertSummaryRecord getSystemAlertSummary() {
        return getObject(alertUrl, "/api/v1/alerts/summary/system", "ALERT-SERVICE", "getSystemAlertSummary", AlertSummaryRecord.class, null);
    }

    public AlertSummaryRecord getMyAlertSummary() {
        return getObject(alertUrl, "/api/v1/alerts/summary/my", "ALERT-SERVICE", "getMyAlertSummary", AlertSummaryRecord.class, null);
    }

    public List<AlertRecord> getRecentAlerts(boolean admin) {
        return getPage(alertUrl, admin ? "/api/v1/alerts/search" : "/api/v1/alerts/my", "ALERT-SERVICE", "getRecentAlerts", ALERT_PAGE, builder -> builder
                .queryParam("page", 0)
                .queryParam("size", 5)
                .queryParam("sortBy", "createdAt")
                .queryParam("sortDir", "desc")).content();
    }

    private void applyFilterRange(ReportFilterRequest filter, UriBuilder builder) {
        if (filter.getFromDate() != null) {
            builder.queryParam("fromDate", filter.getFromDate().atStartOfDay());
        }
        if (filter.getToDate() != null) {
            builder.queryParam("toDate", filter.getToDate().plusDays(1).atStartOfDay().minusNanos(1));
        }
    }

    private <T> PageResponse<T> getPage(
            String baseUrl,
            String path,
            String serviceName,
            String methodName,
            ParameterizedTypeReference<PageResponse<T>> type,
            Consumer<UriBuilder> customizer) {
        PageResponse<T> page = webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path(path);
                    if (customizer != null) {
                        customizer.accept(builder);
                    }
                    return builder.build();
                })
                .headers(this::applyAuth)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(downstreamError(
                                        serviceName,
                                        methodName,
                                        buildUrl(baseUrl, path, customizer),
                                        response.statusCode().value(),
                                        body)));
                    }
                    return response.bodyToMono(type);
                })
                .block();
        if (page == null) {
            return new PageResponse<>(Collections.emptyList(), 0L, 0, 0, FETCH_SIZE);
        }
        return page;
    }

    private <T> List<T> getList(
            String baseUrl,
            String path,
            String serviceName,
            String methodName,
            ParameterizedTypeReference<List<T>> type,
            Consumer<UriBuilder> customizer) {
        List<T> list = webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path(path);
                    if (customizer != null) {
                        customizer.accept(builder);
                    }
                    return builder.build();
                })
                .headers(this::applyAuth)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(downstreamError(
                                        serviceName,
                                        methodName,
                                        buildUrl(baseUrl, path, customizer),
                                        response.statusCode().value(),
                                        body)));
                    }
                    return response.bodyToMono(type);
                })
                .block();
        return list != null ? list : List.of();
    }

    private <T> T getObject(
            String baseUrl,
            String path,
            String serviceName,
            String methodName,
            Class<T> type,
            Consumer<UriBuilder> customizer) {
        T value = webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path(path);
                    if (customizer != null) {
                        customizer.accept(builder);
                    }
                    return builder.build();
                })
                .headers(this::applyAuth)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(downstreamError(
                                        serviceName,
                                        methodName,
                                        buildUrl(baseUrl, path, customizer),
                                        response.statusCode().value(),
                                        body)));
                    }
                    return response.bodyToMono(type);
                })
                .block();
        if (value == null) {
            throw new ReportGenerationException("Received empty response from downstream service");
        }
        return value;
    }

    private void applyAuth(HttpHeaders headers) {
        String token = resolveToken();
        if (StringUtils.hasText(token)) {
            headers.setBearerAuth(token);
        }
    }

    private String resolveToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.token();
        }
        if (StringUtils.hasText(internalServiceToken)) {
            return internalServiceToken;
        }
        return null;
    }

    private String buildUrl(String baseUrl, String path, Consumer<UriBuilder> customizer) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl).path(path);
        if (customizer != null) {
            customizer.accept(builder);
        }
        return builder.build().toUriString();
    }

    private ReportGenerationException downstreamError(
            String serviceName,
            String methodName,
            String url,
            int status,
            String body) {
        String trimmedBody = body == null ? "" : body.trim();
        log.error(
                "Failed downstream call in {} to {} endpoint {} status={} body={}",
                methodName,
                serviceName,
                url,
                status,
                trimmedBody);
        return new ReportGenerationException(
                "Failed to call " + serviceName + " from " + methodName + ": " + url + ", status=" + status
                        + (trimmedBody.isEmpty() ? "" : ", body=" + trimmedBody));
    }

    public record PageResponse<T>(List<T> content, Long totalElements, Integer totalPages, Integer number, Integer size) {
    }

    public record ProductRecord(Long productId, String sku, String name, String category, String brand, BigDecimal costPrice,
                                BigDecimal sellingPrice, Integer reorderLevel, Integer maxStockLevel, Boolean isActive) {
    }

    public record WarehouseRecord(Long warehouseId, String name, String code, Integer capacity, Integer usedCapacity, Boolean isActive) {
    }

    public record StockRecord(Long stockId, Long warehouseId, String warehouseName, Long productId, String productName, String sku,
                              Integer quantity, Integer reservedQuantity, Integer availableQuantity, String locationCode) {
    }

    public record MovementRecord(Long movementId, String movementNumber, Long productId, String productSku, String productName,
                                 Long warehouseId, String warehouseName, String warehouseCode, String movementType, String direction,
                                 BigDecimal quantity, BigDecimal unitCost, BigDecimal totalValue, String referenceType, String referenceNumber,
                                 Long performedBy, LocalDateTime movementDate) {
    }

    public record PurchaseOrderRecord(Long purchaseOrderId, String poNumber, Long supplierId, String supplierName, Long warehouseId,
                                      String warehouseName, String status, BigDecimal totalAmount, LocalDate expectedDeliveryDate,
                                      LocalDate actualDeliveryDate, Boolean isOverdue, LocalDateTime createdAt, LocalDateTime receivedAt) {
    }

    public record SupplierRecord(Long supplierId, String name, Integer leadTimeDays, BigDecimal rating, Boolean isActive) {
    }

    public record PaymentRecord(Long paymentId, String paymentNumber, Long purchaseOrderId, Long supplierId, String supplierName,
                                String status, BigDecimal paymentAmount, BigDecimal remainingAmount) {
    }

    public record PaymentSummaryRecord(Long totalPayments, Long draftCount, Long pendingApprovalCount, Long approvedCount,
                                       Long partiallyPaidCount, Long paidCount, Long cancelledCount, Long rejectedCount,
                                       Long reversedCount, BigDecimal totalPaidAmount, BigDecimal pendingPaymentAmount,
                                       BigDecimal remainingPaymentAmount) {
    }

    public record AlertSummaryRecord(Long totalAlerts, Long unreadCount, Long acknowledgedCount, Long dismissedCount,
                                     Long criticalCount, Long warningCount, Long infoCount, Long lowStockCount,
                                     Long overstockCount, Long pendingPoApprovalCount, Long overduePoCount) {
    }

    public record AlertRecord(Long alertId, String title, String severity, String type, LocalDateTime createdAt) {
    }
}
