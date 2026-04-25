package com.stockpro.web.service;

import com.stockpro.web.dto.request.AlertSearchRequest;
import com.stockpro.web.dto.request.MovementSearchRequest;
import com.stockpro.web.dto.request.ProductFormRequest;
import com.stockpro.web.dto.request.ProductSearchRequest;
import com.stockpro.web.dto.request.ReportFilterRequest;
import com.stockpro.web.dto.request.StockSearchRequest;
import com.stockpro.web.dto.request.TransferStockRequest;
import com.stockpro.web.dto.response.AlertResponse;
import com.stockpro.web.dto.response.LowStockReportResponse;
import com.stockpro.web.dto.response.MovementResponse;
import com.stockpro.web.dto.response.ProductResponse;
import com.stockpro.web.dto.response.StockLevelResponse;
import com.stockpro.web.dto.response.TopMovingProductResponse;
import com.stockpro.web.dto.response.TransferStockResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import com.stockpro.web.viewmodel.DashboardViewModel;
import com.stockpro.web.viewmodel.PageViewModel;
import com.stockpro.web.viewmodel.ProductDetailViewModel;
import com.stockpro.web.viewmodel.ReportsDashboardViewModel;
import com.stockpro.web.viewmodel.WarehouseDetailViewModel;
import java.util.List;

public interface InventoryWebService {

    DashboardViewModel getDashboard();

    PageViewModel<ProductResponse> getProducts(ProductSearchRequest request);

    ProductDetailViewModel getProductDetail(Long productId);

    ProductFormRequest getProductForm(Long productId);

    ProductResponse scanBarcode(String barcode);

    ProductResponse createProduct(ProductFormRequest request);

    ProductResponse updateProduct(Long productId, ProductFormRequest request);

    ProductResponse deactivateProduct(Long productId);

    PageViewModel<WarehouseResponse> getWarehouses(int page, int size);

    WarehouseDetailViewModel getWarehouseDetail(Long warehouseId);

    PageViewModel<StockLevelResponse> getStockLevels(StockSearchRequest request);

    TransferStockResponse transferStock(TransferStockRequest request);

    PageViewModel<MovementResponse> getMovements(MovementSearchRequest request);

    PageViewModel<AlertResponse> getAlerts(AlertSearchRequest request, Long currentUserId, boolean admin);

    AlertResponse markAlertRead(Long alertId);

    AlertResponse acknowledgeAlert(Long alertId);

    ReportsDashboardViewModel getReportsDashboard();

    PageViewModel<LowStockReportResponse> getLowStockReport(ReportFilterRequest request);

    PageViewModel<TopMovingProductResponse> getTopMovingProducts(ReportFilterRequest request);

    PageViewModel<com.stockpro.web.dto.response.DeadStockResponse> getDeadStockReport(ReportFilterRequest request);

    List<ProductResponse> getProductOptions();

    List<WarehouseResponse> getWarehouseOptions();
}
