package com.stockpro.web.service.impl;

import com.stockpro.web.client.AlertServiceClient;
import com.stockpro.web.client.MovementServiceClient;
import com.stockpro.web.client.ProductServiceClient;
import com.stockpro.web.client.ReportServiceClient;
import com.stockpro.web.client.WarehouseServiceClient;
import com.stockpro.web.dto.request.AlertSearchRequest;
import com.stockpro.web.dto.request.MovementSearchRequest;
import com.stockpro.web.dto.request.ProductFormRequest;
import com.stockpro.web.dto.request.ProductSearchRequest;
import com.stockpro.web.dto.request.ReportFilterRequest;
import com.stockpro.web.dto.request.StockSearchRequest;
import com.stockpro.web.dto.request.TransferStockRequest;
import com.stockpro.web.dto.response.AlertResponse;
import com.stockpro.web.dto.response.ApiPageResponse;
import com.stockpro.web.dto.response.DeadStockResponse;
import com.stockpro.web.dto.response.LowStockReportResponse;
import com.stockpro.web.dto.response.MovementResponse;
import com.stockpro.web.dto.response.ProductResponse;
import com.stockpro.web.dto.response.StockLevelResponse;
import com.stockpro.web.dto.response.TopMovingProductResponse;
import com.stockpro.web.dto.response.TransferStockResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import com.stockpro.web.dto.response.WarehouseUtilizationResponse;
import com.stockpro.web.service.InventoryWebService;
import com.stockpro.web.viewmodel.DashboardViewModel;
import com.stockpro.web.viewmodel.MetricCardViewModel;
import com.stockpro.web.viewmodel.PageViewModel;
import com.stockpro.web.viewmodel.ProductDetailViewModel;
import com.stockpro.web.viewmodel.QuickActionViewModel;
import com.stockpro.web.viewmodel.ReportCardViewModel;
import com.stockpro.web.viewmodel.ReportsDashboardViewModel;
import com.stockpro.web.viewmodel.WarehouseDetailViewModel;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InventoryWebServiceImpl implements InventoryWebService {

    private static final int OPTION_PAGE_SIZE = 200;

    private final ProductServiceClient productServiceClient;
    private final WarehouseServiceClient warehouseServiceClient;
    private final MovementServiceClient movementServiceClient;
    private final AlertServiceClient alertServiceClient;
    private final ReportServiceClient reportServiceClient;

    public InventoryWebServiceImpl(ProductServiceClient productServiceClient,
            WarehouseServiceClient warehouseServiceClient,
            MovementServiceClient movementServiceClient,
            AlertServiceClient alertServiceClient,
            ReportServiceClient reportServiceClient) {
        this.productServiceClient = productServiceClient;
        this.warehouseServiceClient = warehouseServiceClient;
        this.movementServiceClient = movementServiceClient;
        this.alertServiceClient = alertServiceClient;
        this.reportServiceClient = reportServiceClient;
    }

    @Override
    public DashboardViewModel getDashboard() {
        ReportFilterRequest baseFilter = new ReportFilterRequest();
        BigDecimal totalStockValue = reportServiceClient.getTotalStockValue(null).getTotalStockValue();
        ApiPageResponse<LowStockReportResponse> lowStock = reportServiceClient.getLowStockReport(baseFilter);
        ApiPageResponse<DeadStockResponse> deadStock = reportServiceClient.getDeadStock(baseFilter);
        BigDecimal totalSpend = reportServiceClient.getPurchaseOrderSummary(baseFilter).getTotalSpend();

        List<MetricCardViewModel> metrics = List.of(
                new MetricCardViewModel("Total Stock Value", currency(totalStockValue), "success",
                        "Live valuation from report-service.", "/inventory/reports"),
                new MetricCardViewModel("Low Stock Count", String.valueOf(lowStock.getTotalElements()), "warning",
                        "Critical replenishment candidates.", "/inventory/reports/low-stock"),
                new MetricCardViewModel("Dead Stock Count", String.valueOf(deadStock.getTotalElements()), "critical",
                        "Items with no recent movement.", "/inventory/reports/dead-stock"),
                new MetricCardViewModel("Total PO Spend", currency(totalSpend), "neutral",
                        "Current purchase order spend window.", "/purchase/orders"));

        List<QuickActionViewModel> quickActions = List.of(
                new QuickActionViewModel("Products", "Browse catalogue and maintain item master data.",
                        "/inventory/products", "box", "neutral"),
                new QuickActionViewModel("Warehouses", "Review utilization and location capacity.",
                        "/inventory/warehouses", "warehouse", "success"),
                new QuickActionViewModel("Transfers", "Move stock across warehouse locations.",
                        "/inventory/stock/transfer", "swap", "warning"),
                new QuickActionViewModel("Alerts", "Review unread platform and stock alerts.",
                        "/inventory/alerts", "bell", "critical"));

        return new DashboardViewModel(metrics, quickActions);
    }

    @Override
    public PageViewModel<ProductResponse> getProducts(ProductSearchRequest request) {
        if (hasProductFilters(request)) {
            return PageViewModel.from(productServiceClient.searchProducts(request));
        }
        return PageViewModel.from(productServiceClient.getAllProducts(
                request.getPage(),
                request.getSize(),
                safe(request.getSortBy(), "productId"),
                safe(request.getSortDir(), "asc")));
    }

    @Override
    public ProductDetailViewModel getProductDetail(Long productId) {
        ProductResponse product = productServiceClient.getProductById(productId);
        PageViewModel<StockLevelResponse> stockLevels = PageViewModel.from(
                warehouseServiceClient.getStockByProduct(productId, 0, 20, "lastUpdated", "desc"));

        Map<Long, WarehouseResponse> warehousesById = getWarehouseOptions().stream()
                .collect(LinkedHashMap::new,
                        (map, warehouse) -> map.put(warehouse.getWarehouseId(), warehouse),
                        Map::putAll);

        return new ProductDetailViewModel(product, stockLevels, warehousesById);
    }

    @Override
    public ProductFormRequest getProductForm(Long productId) {
        if (productId == null) {
            return new ProductFormRequest();
        }

        ProductResponse product = productServiceClient.getProductById(productId);
        ProductFormRequest form = new ProductFormRequest();
        form.setProductId(product.getProductId());
        form.setSku(product.getSku());
        form.setName(product.getName());
        form.setDescription(product.getDescription());
        form.setCategory(product.getCategory());
        form.setBrand(product.getBrand());
        form.setUnitOfMeasure(product.getUnitOfMeasure());
        form.setCostPrice(product.getCostPrice());
        form.setSellingPrice(product.getSellingPrice());
        form.setReorderLevel(product.getReorderLevel());
        form.setMaxStockLevel(product.getMaxStockLevel());
        form.setLeadTimeDays(product.getLeadTimeDays());
        form.setImageUrl(product.getImageUrl());
        form.setBarcode(product.getBarcode());
        form.setActive(product.getActive());
        return form;
    }

    @Override
    public ProductResponse scanBarcode(String barcode) {
        return productServiceClient.getProductByBarcode(barcode);
    }

    @Override
    public ProductResponse createProduct(ProductFormRequest request) {
        return productServiceClient.createProduct(request);
    }

    @Override
    public ProductResponse updateProduct(Long productId, ProductFormRequest request) {
        return productServiceClient.updateProduct(productId, request);
    }

    @Override
    public ProductResponse deactivateProduct(Long productId) {
        return productServiceClient.deactivateProduct(productId);
    }

    @Override
    public PageViewModel<WarehouseResponse> getWarehouses(int page, int size) {
        return PageViewModel.from(warehouseServiceClient.getWarehouses(page, size, "name", "asc"));
    }

    @Override
    public WarehouseDetailViewModel getWarehouseDetail(Long warehouseId) {
        WarehouseResponse warehouse = warehouseServiceClient.getWarehouseById(warehouseId);
        WarehouseUtilizationResponse utilization = warehouseServiceClient.getUtilization(warehouseId);
        PageViewModel<StockLevelResponse> stockLevels = PageViewModel.from(
                warehouseServiceClient.getStockByWarehouse(warehouseId, 0, 20, "lastUpdated", "desc"));
        Map<Long, ProductResponse> productsById = getProductOptions().stream()
                .collect(LinkedHashMap::new,
                        (map, product) -> map.put(product.getProductId(), product),
                        Map::putAll);
        return new WarehouseDetailViewModel(warehouse, utilization, stockLevels, productsById);
    }

    @Override
    public PageViewModel<StockLevelResponse> getStockLevels(StockSearchRequest request) {
        if (request.getWarehouseId() != null && request.getProductId() != null
                && !StringUtils.hasText(request.getLocation())
                && !Boolean.TRUE.equals(request.getLowStockOnly())) {
            return PageViewModel.fromList(List.of(
                    warehouseServiceClient.getStockLevel(request.getWarehouseId(), request.getProductId())),
                    request.getPage(),
                    request.getSize());
        }
        if (request.getWarehouseId() != null && request.getProductId() == null
                && !StringUtils.hasText(request.getLocation()) && !Boolean.TRUE.equals(request.getLowStockOnly())) {
            return PageViewModel.from(warehouseServiceClient.getStockByWarehouse(
                    request.getWarehouseId(), request.getPage(), request.getSize(), "lastUpdated", "desc"));
        }
        if (request.getProductId() != null && request.getWarehouseId() == null
                && !StringUtils.hasText(request.getLocation()) && !Boolean.TRUE.equals(request.getLowStockOnly())) {
            return PageViewModel.from(warehouseServiceClient.getStockByProduct(
                    request.getProductId(), request.getPage(), request.getSize(), "lastUpdated", "desc"));
        }
        return PageViewModel.from(warehouseServiceClient.searchStock(request));
    }

    @Override
    public TransferStockResponse transferStock(TransferStockRequest request) {
        return warehouseServiceClient.transferStock(request);
    }

    @Override
    public PageViewModel<MovementResponse> getMovements(MovementSearchRequest request) {
        if (request.getProductId() == null
                && request.getWarehouseId() == null
                && request.getMovementType() == null
                && request.getStartDate() == null
                && request.getEndDate() == null) {
            return PageViewModel.from(movementServiceClient.getAllMovements(
                    request.getPage(),
                    request.getSize(),
                    safe(request.getSortBy(), "movementDate"),
                    safe(request.getSortDir(), "desc")));
        }
        return PageViewModel.from(movementServiceClient.searchMovements(request));
    }

    @Override
    public PageViewModel<AlertResponse> getAlerts(AlertSearchRequest request, Long currentUserId, boolean admin) {
        Long recipientId = admin ? request.getRecipientId() : currentUserId;
        return PageViewModel.from(alertServiceClient.getAlerts(
                recipientId,
                request.getType(),
                request.getSeverity(),
                request.getRead(),
                request.getAcknowledged(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPage(),
                request.getSize(),
                safe(request.getSortBy(), "createdAt"),
                safe(request.getSortDir(), "desc")));
    }

    @Override
    public AlertResponse markAlertRead(Long alertId) {
        return alertServiceClient.markAsRead(alertId);
    }

    @Override
    public AlertResponse acknowledgeAlert(Long alertId) {
        return alertServiceClient.acknowledge(alertId);
    }

    @Override
    public ReportsDashboardViewModel getReportsDashboard() {
        return new ReportsDashboardViewModel(List.of(
                new ReportCardViewModel("Low Stock", "Prioritize replenishment queues and stock risk.",
                        "/inventory/reports/low-stock", "warning"),
                new ReportCardViewModel("Top Moving", "Identify fast-moving inventory by quantity.",
                        "/inventory/reports/top-moving", "success"),
                new ReportCardViewModel("Dead Stock", "Review inventory with no recent movement.",
                        "/inventory/reports/dead-stock", "critical"),
                new ReportCardViewModel("Admin Reports", "Open advanced valuation and export tools.",
                        "/admin/reports", "neutral")));
    }

    @Override
    public PageViewModel<LowStockReportResponse> getLowStockReport(ReportFilterRequest request) {
        return PageViewModel.from(reportServiceClient.getLowStockReport(request));
    }

    @Override
    public PageViewModel<TopMovingProductResponse> getTopMovingProducts(ReportFilterRequest request) {
        return PageViewModel.from(reportServiceClient.getTopMovingProducts(request));
    }

    @Override
    public PageViewModel<DeadStockResponse> getDeadStockReport(ReportFilterRequest request) {
        return PageViewModel.from(reportServiceClient.getDeadStock(request));
    }

    @Override
    public List<ProductResponse> getProductOptions() {
        return productServiceClient.getAllProducts(0, OPTION_PAGE_SIZE, "name", "asc").getContent();
    }

    @Override
    public List<WarehouseResponse> getWarehouseOptions() {
        return warehouseServiceClient.getWarehouses(0, OPTION_PAGE_SIZE, "name", "asc").getContent();
    }

    private boolean hasProductFilters(ProductSearchRequest request) {
        return StringUtils.hasText(request.getName())
                || StringUtils.hasText(request.getCategory())
                || StringUtils.hasText(request.getBrand())
                || StringUtils.hasText(request.getSku())
                || StringUtils.hasText(request.getBarcode())
                || request.getActive() != null;
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String currency(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(safeValue);
    }
}
