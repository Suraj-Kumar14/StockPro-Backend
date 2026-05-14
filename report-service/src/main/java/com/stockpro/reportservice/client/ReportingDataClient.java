package com.stockpro.reportservice.client;

import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.exception.ReportGenerationException;
import com.stockpro.reportservice.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
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
    private static final String QUERY_PAGE = "page";
    private static final String QUERY_SIZE = "size";
    private static final String QUERY_SORT_BY = "sortBy";
    private static final String QUERY_SORT_DIR = "sortDir";
    private static final String QUERY_WAREHOUSE_ID = "warehouseId";
    private static final String QUERY_PRODUCT_ID = "productId";
    private static final String QUERY_SUPPLIER_ID = "supplierId";
    private static final String QUERY_STATUS = "status";
    private static final String QUERY_FROM_DATE = "fromDate";
    private static final String QUERY_TO_DATE = "toDate";
    private static final String SORT_BY_NAME = "name";
    private static final String SORT_BY_CREATED_AT = "createdAt";
    private static final String SORT_BY_MOVEMENT_DATE = "movementDate";
    private static final String SORT_ASC = "asc";
    private static final String SORT_DESC = "desc";
    private static final String WAREHOUSE_SERVICE = "WAREHOUSE-SERVICE";
    private static final String MOVEMENT_SERVICE = "MOVEMENT-SERVICE";
    private static final String PURCHASE_SERVICE = "PURCHASE-SERVICE";
    private static final String SUPPLIER_SERVICE = "SUPPLIER-SERVICE";
    private static final String PAYMENT_SERVICE = "PAYMENT-SERVICE";
    private static final String ALERT_SERVICE = "ALERT-SERVICE";
    private static final ParameterizedTypeReference<PageResponse<ProductRecord>> PRODUCT_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<WarehouseRecord>> WAREHOUSE_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<StockRecord>> STOCK_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<MovementRecord>> MOVEMENT_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<PurchaseOrderRecord>> PURCHASE_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<PurchaseOrderReportRecord>> PURCHASE_REPORT_PAGE = new ParameterizedTypeReference<>() {};
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
        return getAllPages(productUrl, "/api/v1/products", "PRODUCT-SERVICE", "getProducts", PRODUCT_PAGE,
                builder -> applyPageAndSort(builder, 0, FETCH_SIZE, SORT_BY_NAME, SORT_ASC));
    }

    public List<WarehouseRecord> getWarehouses() {
        return getAllPages(warehouseUrl, "/api/v1/warehouses", WAREHOUSE_SERVICE, "getWarehouses", WAREHOUSE_PAGE,
                builder -> applyPageAndSort(builder, 0, FETCH_SIZE, SORT_BY_NAME, SORT_ASC));
    }

    public List<StockRecord> getStocks(ReportFilterRequest filter) {
        return getAllPages(warehouseUrl, "/api/v1/stocks", WAREHOUSE_SERVICE, "getStocks", STOCK_PAGE, builder -> {
            addOptionalQueryParam(builder, QUERY_WAREHOUSE_ID, filter.getWarehouseId());
            addOptionalQueryParam(builder, QUERY_PRODUCT_ID, filter.getProductId());
            builder.queryParam(QUERY_PAGE, 0).queryParam(QUERY_SIZE, FETCH_SIZE);
        });
    }

    public List<StockRecord> getLowStockItems() {
        return getList(warehouseUrl, "/api/v1/stocks/low-stock", WAREHOUSE_SERVICE, "getLowStockItems", STOCK_LIST, null);
    }

    public List<StockRecord> getOverstockItems() {
        return getList(warehouseUrl, "/api/v1/stocks/overstock", WAREHOUSE_SERVICE, "getOverstockItems", STOCK_LIST, null);
    }

    public PageResponse<MovementRecord> searchMovements(ReportFilterRequest filter) {
        return getPage(movementUrl, "/api/v1/movements/search", MOVEMENT_SERVICE, "searchMovements", MOVEMENT_PAGE, builder -> {
            applyFilterRange(filter, builder);
            addOptionalQueryParam(builder, QUERY_WAREHOUSE_ID, filter.getWarehouseId());
            addOptionalQueryParam(builder, QUERY_PRODUCT_ID, filter.getProductId());
            if (StringUtils.hasText(filter.getMovementType())) {
                builder.queryParam("movementType", filter.getMovementType());
            }
            applyPageAndSort(
                    builder,
                    filter.getPage(),
                    Math.max(filter.getSize(), FETCH_SIZE),
                    StringUtils.hasText(filter.getSortBy()) ? filter.getSortBy() : SORT_BY_MOVEMENT_DATE,
                    StringUtils.hasText(filter.getSortDir()) ? filter.getSortDir() : SORT_DESC);
        });
    }

    public List<MovementRecord> searchAllMovements(ReportFilterRequest filter) {
        return getAllPages(movementUrl, "/api/v1/movements/search", MOVEMENT_SERVICE, "searchAllMovements", MOVEMENT_PAGE, builder -> {
            applyFilterRange(filter, builder);
            addOptionalQueryParam(builder, QUERY_WAREHOUSE_ID, filter.getWarehouseId());
            addOptionalQueryParam(builder, QUERY_PRODUCT_ID, filter.getProductId());
            if (StringUtils.hasText(filter.getMovementType())) {
                builder.queryParam("movementType", filter.getMovementType());
            }
            applyPageAndSort(
                    builder,
                    0,
                    FETCH_SIZE,
                    StringUtils.hasText(filter.getSortBy()) ? filter.getSortBy() : SORT_BY_MOVEMENT_DATE,
                    StringUtils.hasText(filter.getSortDir()) ? filter.getSortDir() : SORT_DESC);
        });
    }

    public List<PurchaseOrderRecord> searchPurchaseOrders(ReportFilterRequest filter) {
        String url = buildUrl(purchaseUrl, "/api/v1/purchase-orders/search", builder -> {
            addOptionalQueryParam(builder, QUERY_SUPPLIER_ID, filter.getSupplierId());
            addOptionalQueryParam(builder, QUERY_WAREHOUSE_ID, filter.getWarehouseId());
            if (StringUtils.hasText(filter.getPoStatus())) {
                builder.queryParam(QUERY_STATUS, filter.getPoStatus());
            }
            applyDateRange(filter, builder);
            applyPageAndSort(builder, 0, FETCH_SIZE, SORT_BY_CREATED_AT, SORT_DESC);
        });
        log.info(
                "Fetching poSummary purchase orders url={} from={} to={} warehouseId={} supplierId={}",
                url,
                filter.getFromDate(),
                filter.getToDate(),
                filter.getWarehouseId(),
                filter.getSupplierId());
        List<PurchaseOrderRecord> orders = getAllPages(purchaseUrl, "/api/v1/purchase-orders/search", PURCHASE_SERVICE, "searchPurchaseOrders", PURCHASE_PAGE, builder -> {
            addOptionalQueryParam(builder, QUERY_SUPPLIER_ID, filter.getSupplierId());
            addOptionalQueryParam(builder, QUERY_WAREHOUSE_ID, filter.getWarehouseId());
            if (StringUtils.hasText(filter.getPoStatus())) {
                builder.queryParam(QUERY_STATUS, filter.getPoStatus());
            }
            applyDateRange(filter, builder);
            applyPageAndSort(builder, 0, FETCH_SIZE, SORT_BY_CREATED_AT, SORT_DESC);
        });
        log.info("Fetched poSummary purchase orders url={} count={}", url, orders.size());
        return orders;
    }

    public List<PurchaseOrderReportRecord> getPurchaseOrderReportRows(ReportFilterRequest filter) {
        return getAllPages(purchaseUrl, "/api/v1/purchase-orders/reports", PURCHASE_SERVICE, "getPurchaseOrderReportRows", PURCHASE_REPORT_PAGE, builder -> {
            addOptionalQueryParam(builder, QUERY_SUPPLIER_ID, filter.getSupplierId());
            if (StringUtils.hasText(filter.getPoStatus())) {
                builder.queryParam(QUERY_STATUS, filter.getPoStatus());
            }
            if (StringUtils.hasText(filter.getPaymentStatus())) {
                builder.queryParam("paymentStatus", filter.getPaymentStatus());
            }
            applyDateRange(filter, builder);
            builder.queryParam(QUERY_PAGE, 0).queryParam(QUERY_SIZE, FETCH_SIZE);
        });
    }

    public PurchaseOrderDetailRecord getPurchaseOrder(Long purchaseOrderId) {
        return getObject(purchaseUrl, "/api/v1/purchase-orders/" + purchaseOrderId, PURCHASE_SERVICE, "getPurchaseOrder",
                PurchaseOrderDetailRecord.class, null);
    }

    public List<SupplierRecord> getSuppliers() {
        return getAllPages(supplierUrl, "/api/v1/suppliers", SUPPLIER_SERVICE, "getSuppliers", SUPPLIER_PAGE,
                builder -> applyPageAndSort(builder, 0, FETCH_SIZE, SORT_BY_NAME, SORT_ASC));
    }

    public List<PaymentRecord> searchPayments(ReportFilterRequest filter) {
        return getAllPages(paymentUrl, "/api/v1/payments/search", PAYMENT_SERVICE, "searchPayments", PAYMENT_PAGE, builder -> {
            addOptionalQueryParam(builder, QUERY_SUPPLIER_ID, filter.getSupplierId());
            if (StringUtils.hasText(filter.getPaymentStatus())) {
                builder.queryParam(QUERY_STATUS, filter.getPaymentStatus());
            }
            applyDateRange(filter, builder);
            applyPageAndSort(builder, 0, FETCH_SIZE, SORT_BY_CREATED_AT, SORT_DESC);
        });
    }

    public List<PaymentRecord> getPaymentsByPurchaseOrder(Long purchaseOrderId) {
        return getAllPages(paymentUrl, "/api/v1/payments/purchase-order/" + purchaseOrderId, PAYMENT_SERVICE, "getPaymentsByPurchaseOrder",
                PAYMENT_PAGE, builder -> builder
                        .queryParam(QUERY_PAGE, 0)
                        .queryParam(QUERY_SIZE, FETCH_SIZE));
    }

    public RemainingAmountRecord getRemainingAmount(Long purchaseOrderId) {
        return getObject(paymentUrl, "/api/v1/payments/purchase-order/" + purchaseOrderId + "/remaining-amount", PAYMENT_SERVICE,
                "getRemainingAmount", RemainingAmountRecord.class, null);
    }

    public PaymentSummaryRecord getPaymentSummary() {
        return getObject(paymentUrl, "/api/v1/payments/summary", PAYMENT_SERVICE, "getPaymentSummary", PaymentSummaryRecord.class, null);
    }

    public AlertSummaryRecord getSystemAlertSummary() {
        return getObject(alertUrl, "/api/v1/alerts/summary/system", ALERT_SERVICE, "getSystemAlertSummary", AlertSummaryRecord.class, null);
    }

    public AlertSummaryRecord getMyAlertSummary() {
        return getObject(alertUrl, "/api/v1/alerts/summary/my", ALERT_SERVICE, "getMyAlertSummary", AlertSummaryRecord.class, null);
    }

    public List<AlertRecord> getRecentAlerts(boolean admin) {
        return getPage(alertUrl, admin ? "/api/v1/alerts/search" : "/api/v1/alerts/my", ALERT_SERVICE, "getRecentAlerts", ALERT_PAGE,
                builder -> applyPageAndSort(builder, 0, 5, SORT_BY_CREATED_AT, SORT_DESC)).content();
    }

    private void applyFilterRange(ReportFilterRequest filter, UriBuilder builder) {
        if (filter.getFromDate() != null) {
            builder.queryParam(QUERY_FROM_DATE, filter.getFromDate().atStartOfDay());
        }
        if (filter.getToDate() != null) {
            builder.queryParam(QUERY_TO_DATE, filter.getToDate().plusDays(1).atStartOfDay().minusNanos(1));
        }
    }

    private void applyDateRange(ReportFilterRequest filter, UriBuilder builder) {
        addOptionalQueryParam(builder, QUERY_FROM_DATE, filter.getFromDate());
        addOptionalQueryParam(builder, QUERY_TO_DATE, filter.getToDate());
    }

    private void addOptionalQueryParam(UriBuilder builder, String name, Object value) {
        if (value != null) {
            builder.queryParam(name, value);
        }
    }

    private void applyPageAndSort(UriBuilder builder, int page, int size, String sortBy, String sortDir) {
        builder.queryParam(QUERY_PAGE, page)
                .queryParam(QUERY_SIZE, size)
                .queryParam(QUERY_SORT_BY, sortBy)
                .queryParam(QUERY_SORT_DIR, sortDir);
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

    private <T> List<T> getAllPages(
            String baseUrl,
            String path,
            String serviceName,
            String methodName,
            ParameterizedTypeReference<PageResponse<T>> type,
            Consumer<UriBuilder> customizer) {
        List<T> content = new ArrayList<>();
        int currentPage = 0;
        int totalPages;
        do {
            final int pageNumber = currentPage;
            PageResponse<T> response = getPage(baseUrl, path, serviceName, methodName, type, builder -> {
                if (customizer != null) {
                    customizer.accept(builder);
                }
                builder.replaceQueryParam("page", pageNumber);
                builder.replaceQueryParam("size", FETCH_SIZE);
            });
            content.addAll(response.content() != null ? response.content() : List.of());
            totalPages = response.totalPages() != null ? response.totalPages() : 0;
            currentPage++;
        } while (currentPage < Math.max(totalPages, 1));
        return content;
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

    public record PurchaseOrderLineItemRecord(Long lineItemId, Long productId, String productSku, String productName,
                                              Integer orderedQuantity, Integer receivedQuantity, Integer pendingQuantity,
                                              BigDecimal unitCost, BigDecimal lineTotal, String notes) {
    }

    public record PurchaseOrderHistoryRecord(Long historyId, String action, String oldStatus, String newStatus, Long actorId,
                                             String remarks, LocalDateTime actionAt) {
    }

    public record PurchaseOrderDetailRecord(Long purchaseOrderId, String poNumber, Long supplierId, String supplierName,
                                            Long warehouseId, String warehouseName, Long createdBy, String createdByName,
                                            Long approvedBy, String approvedByName, String status, BigDecimal subtotalAmount,
                                            BigDecimal taxAmount, BigDecimal discountAmount, BigDecimal shippingAmount,
                                            BigDecimal totalAmount, LocalDate expectedDeliveryDate, LocalDate actualDeliveryDate,
                                            String paymentTerms, String notes, String approvalRemarks, String rejectionReason,
                                            String cancellationReason, LocalDateTime submittedAt, LocalDateTime approvedAt,
                                            LocalDateTime rejectedAt, LocalDateTime cancelledAt, LocalDateTime receivedAt,
                                            LocalDateTime createdAt, LocalDateTime updatedAt, Boolean isOverdue,
                                            String paymentStatus, Boolean paymentCompleted,
                                            List<PurchaseOrderLineItemRecord> lineItems,
                                            List<PurchaseOrderHistoryRecord> history) {
    }

    public record PurchaseOrderReportRecord(Long purchaseOrderId, String poNumber, String purchaseOrderStatus, String paymentStatus,
                                            String paymentNumber, String razorpayOrderId, String razorpayPaymentId,
                                            BigDecimal paymentAmount, LocalDateTime paidAt, Long supplierId, String supplierName,
                                            Long warehouseId, String warehouseName, Long productId, String productSku,
                                            String productName, String productCategory, BigDecimal unitPrice, Integer orderedQuantity,
                                            Integer receivedQuantity, Integer remainingQuantity, BigDecimal lineTotal,
                                            BigDecimal purchaseOrderTotalAmount, LocalDate orderDate, LocalDate expectedDate,
                                            Long approvedBy, LocalDateTime approvedAt, LocalDateTime createdAt) {
    }

    public record SupplierRecord(Long supplierId, String name, Integer leadTimeDays, BigDecimal rating, Boolean isActive) {
    }

    public record PaymentRecord(Long paymentId, String paymentNumber, Long purchaseOrderId, String poNumber, Long supplierId, String supplierName,
                                String status, String paymentMethod, BigDecimal paymentAmount, BigDecimal poTotalAmount,
                                BigDecimal previouslyPaidAmount, BigDecimal remainingAmount, String currency, LocalDate paymentDate,
                                String transactionReference, String razorpayOrderId, String razorpayPaymentId,
                                Long createdBy, Long paidBy, LocalDateTime paidAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record PaymentSummaryRecord(Long totalPayments, Long draftCount, Long pendingApprovalCount, Long approvedCount,
                                       Long partiallyPaidCount, Long paidCount, Long cancelledCount, Long rejectedCount,
                                       Long reversedCount, BigDecimal totalPaidAmount, BigDecimal pendingPaymentAmount,
                                       BigDecimal remainingPaymentAmount) {
    }

    public record RemainingAmountRecord(Long purchaseOrderId, String poNumber, BigDecimal purchaseOrderTotalAmount,
                                        BigDecimal paidAmount, BigDecimal remainingAmount, String status) {
    }

    public record AlertSummaryRecord(Long totalAlerts, Long unreadCount, Long acknowledgedCount, Long dismissedCount,
                                     Long criticalCount, Long warningCount, Long infoCount, Long lowStockCount,
                                     Long overstockCount, Long pendingPoApprovalCount, Long overduePoCount) {
    }

    public record AlertRecord(Long alertId, String title, String severity, String type, LocalDateTime createdAt) {
    }
}
