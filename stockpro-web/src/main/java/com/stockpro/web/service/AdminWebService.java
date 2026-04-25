package com.stockpro.web.service;

import com.stockpro.web.dto.request.GenerateReportRequest;
import com.stockpro.web.dto.request.PlatformAlertRequest;
import com.stockpro.web.dto.request.RegisterUserRequest;
import com.stockpro.web.dto.request.ReportFilterRequest;
import com.stockpro.web.dto.request.UpdateUserRequest;
import com.stockpro.web.dto.request.WarehouseFormRequest;
import com.stockpro.web.dto.response.AlertResponse;
import com.stockpro.web.dto.response.DeadStockResponse;
import com.stockpro.web.dto.response.GeneratedReportResponse;
import com.stockpro.web.dto.response.InventoryTurnoverResponse;
import com.stockpro.web.dto.response.TopMovingProductResponse;
import com.stockpro.web.dto.response.TotalStockValueResponse;
import com.stockpro.web.dto.response.UserResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import com.stockpro.web.viewmodel.AdminDashboardViewModel;
import com.stockpro.web.viewmodel.AuditEntryViewModel;
import com.stockpro.web.viewmodel.PageViewModel;
import java.time.LocalDate;
import java.util.List;

public interface AdminWebService {

    AdminDashboardViewModel getAdminDashboard();

    List<UserResponse> getUsers();

    UserResponse getUser(Long userId);

    String addUser(RegisterUserRequest request);

    UserResponse updateUser(Long userId, UpdateUserRequest request);

    void deactivateUser(Long userId);

    PageViewModel<WarehouseResponse> getWarehouses(int page, int size);

    WarehouseFormRequest getWarehouseForm(Long warehouseId);

    WarehouseResponse addWarehouse(WarehouseFormRequest request);

    WarehouseResponse updateWarehouse(Long warehouseId, WarehouseFormRequest request);

    WarehouseResponse deactivateWarehouse(Long warehouseId);

    TotalStockValueResponse getTotalStockValue(LocalDate snapshotDate);

    List<InventoryTurnoverResponse> getInventoryTurnover(ReportFilterRequest request);

    PageViewModel<TopMovingProductResponse> getTopMovingProducts(ReportFilterRequest request);

    PageViewModel<DeadStockResponse> getDeadStock(ReportFilterRequest request);

    GeneratedReportResponse generateInventoryReport(GenerateReportRequest request);

    AlertResponse sendPlatformAlert(PlatformAlertRequest request);

    List<AuditEntryViewModel> getAuditLogs();
}
