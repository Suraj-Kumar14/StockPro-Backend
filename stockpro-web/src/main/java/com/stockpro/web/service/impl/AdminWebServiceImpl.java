package com.stockpro.web.service.impl;

import com.stockpro.web.client.AlertServiceClient;
import com.stockpro.web.client.AuthServiceClient;
import com.stockpro.web.client.MovementServiceClient;
import com.stockpro.web.client.ReportServiceClient;
import com.stockpro.web.client.WarehouseServiceClient;
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
import com.stockpro.web.dto.response.MovementResponse;
import com.stockpro.web.dto.response.TopMovingProductResponse;
import com.stockpro.web.dto.response.TotalStockValueResponse;
import com.stockpro.web.dto.response.UserResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import com.stockpro.web.service.AdminWebService;
import com.stockpro.web.viewmodel.AdminDashboardViewModel;
import com.stockpro.web.viewmodel.AuditEntryViewModel;
import com.stockpro.web.viewmodel.MetricCardViewModel;
import com.stockpro.web.viewmodel.PageViewModel;
import com.stockpro.web.viewmodel.QuickActionViewModel;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AdminWebServiceImpl implements AdminWebService {

    private final AuthServiceClient authServiceClient;
    private final WarehouseServiceClient warehouseServiceClient;
    private final AlertServiceClient alertServiceClient;
    private final ReportServiceClient reportServiceClient;
    private final MovementServiceClient movementServiceClient;

    public AdminWebServiceImpl(AuthServiceClient authServiceClient,
            WarehouseServiceClient warehouseServiceClient,
            AlertServiceClient alertServiceClient,
            ReportServiceClient reportServiceClient,
            MovementServiceClient movementServiceClient) {
        this.authServiceClient = authServiceClient;
        this.warehouseServiceClient = warehouseServiceClient;
        this.alertServiceClient = alertServiceClient;
        this.reportServiceClient = reportServiceClient;
        this.movementServiceClient = movementServiceClient;
    }

    @Override
    public AdminDashboardViewModel getAdminDashboard() {
        List<UserResponse> users = getUsers();
        PageViewModel<WarehouseResponse> warehouses = getWarehouses(0, 12);
        TotalStockValueResponse stockValue = reportServiceClient.getTotalStockValue(null);
        long riskItems = reportServiceClient.getLowStockReport(new ReportFilterRequest()).getTotalElements()
                + reportServiceClient.getDeadStock(new ReportFilterRequest()).getTotalElements();

        List<MetricCardViewModel> metrics = List.of(
                new MetricCardViewModel("Users", String.valueOf(users.size()), "neutral",
                        "Current identities available from auth-service.", "/admin/users"),
                new MetricCardViewModel("Warehouses", String.valueOf(warehouses.totalElements()), "success",
                        "Warehouse master records exposed by warehouse-service.", "/admin/warehouses"),
                new MetricCardViewModel("Total Stock Value", currency(stockValue.getTotalStockValue()), "warning",
                        "Portfolio valuation snapshot from report-service.", "/admin/reports"),
                new MetricCardViewModel("Risk Items", String.valueOf(riskItems), "critical",
                        "Combined low-stock and dead-stock count.", "/admin/reports"));

        List<QuickActionViewModel> quickActions = List.of(
                new QuickActionViewModel("Manage Users", "Review active accounts and deactivate access.",
                        "/admin/users", "users", "neutral"),
                new QuickActionViewModel("Manage Warehouses", "Maintain location master data.",
                        "/admin/warehouses", "warehouse", "success"),
                new QuickActionViewModel("Generate Report", "Export analytical snapshots through report-service.",
                        "/admin/reports/generate", "report", "warning"),
                new QuickActionViewModel("Send Alert", "Broadcast platform messages to internal users.",
                        "/admin/alerts/send", "bell", "critical"));

        return new AdminDashboardViewModel(metrics, quickActions, getAuditLogs());
    }

    @Override
    public List<UserResponse> getUsers() {
        return authServiceClient.getAllUsers().stream()
                .sorted(Comparator.comparing(UserResponse::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public UserResponse getUser(Long userId) {
        return getUsers().stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Override
    public String addUser(RegisterUserRequest request) {
        return authServiceClient.registerRequest(request);
    }

    @Override
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        UserResponse user = getUser(userId);
        return authServiceClient.updateProfile(user.getEmail(), request);
    }

    @Override
    public void deactivateUser(Long userId) {
        authServiceClient.deactivateUser(userId);
    }

    @Override
    public PageViewModel<WarehouseResponse> getWarehouses(int page, int size) {
        return PageViewModel.from(warehouseServiceClient.getWarehouses(page, size, "name", "asc"));
    }

    @Override
    public WarehouseFormRequest getWarehouseForm(Long warehouseId) {
        if (warehouseId == null) {
            return new WarehouseFormRequest();
        }
        WarehouseResponse warehouse = warehouseServiceClient.getWarehouseById(warehouseId);
        WarehouseFormRequest form = new WarehouseFormRequest();
        form.setWarehouseId(warehouse.getWarehouseId());
        form.setName(warehouse.getName());
        form.setLocation(warehouse.getLocation());
        form.setAddress(warehouse.getAddress());
        form.setManagerId(warehouse.getManagerId());
        form.setCapacity(warehouse.getCapacity());
        form.setPhone(warehouse.getPhone());
        form.setActive(warehouse.getActive());
        return form;
    }

    @Override
    public WarehouseResponse addWarehouse(WarehouseFormRequest request) {
        return warehouseServiceClient.createWarehouse(request);
    }

    @Override
    public WarehouseResponse updateWarehouse(Long warehouseId, WarehouseFormRequest request) {
        return warehouseServiceClient.updateWarehouse(warehouseId, request);
    }

    @Override
    public WarehouseResponse deactivateWarehouse(Long warehouseId) {
        return warehouseServiceClient.deactivateWarehouse(warehouseId);
    }

    @Override
    public TotalStockValueResponse getTotalStockValue(LocalDate snapshotDate) {
        return reportServiceClient.getTotalStockValue(snapshotDate);
    }

    @Override
    public List<InventoryTurnoverResponse> getInventoryTurnover(ReportFilterRequest request) {
        return reportServiceClient.getInventoryTurnover(request);
    }

    @Override
    public PageViewModel<TopMovingProductResponse> getTopMovingProducts(ReportFilterRequest request) {
        return PageViewModel.from(reportServiceClient.getTopMovingProducts(request));
    }

    @Override
    public PageViewModel<DeadStockResponse> getDeadStock(ReportFilterRequest request) {
        return PageViewModel.from(reportServiceClient.getDeadStock(request));
    }

    @Override
    public GeneratedReportResponse generateInventoryReport(GenerateReportRequest request) {
        return reportServiceClient.generateInventoryReport(request);
    }

    @Override
    public AlertResponse sendPlatformAlert(PlatformAlertRequest request) {
        return alertServiceClient.sendAlert(request);
    }

    @Override
    public List<AuditEntryViewModel> getAuditLogs() {
        List<AuditEntryViewModel> movementEntries = movementServiceClient.getAllMovements(0, 8, "createdAt", "desc")
                .getContent()
                .stream()
                .map(this::mapMovementAudit)
                .toList();

        List<AuditEntryViewModel> alertEntries = alertServiceClient.getAlerts(
                null, null, null, null, null, null, null, 0, 8, "createdAt", "desc")
                .getContent()
                .stream()
                .map(alert -> new AuditEntryViewModel(
                        "Alert",
                        alert.getTitle(),
                        alert.getMessage(),
                        alert.getCreatedAt(),
                        severityTone(alert.getSeverity() == null ? null : alert.getSeverity().name())))
                .toList();

        return java.util.stream.Stream.concat(movementEntries.stream(), alertEntries.stream())
                .sorted(Comparator.comparing(AuditEntryViewModel::timestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList();
    }

    private AuditEntryViewModel mapMovementAudit(MovementResponse movement) {
        return new AuditEntryViewModel(
                "Movement",
                movement.getMovementType() == null ? "Stock Event" : movement.getMovementType().name(),
                "Product #" + movement.getProductId() + " moved in warehouse #" + movement.getWarehouseId()
                        + " with reference " + movement.getReferenceType(),
                movement.getCreatedAt() == null ? movement.getMovementDate() : movement.getCreatedAt(),
                "neutral");
    }

    private String severityTone(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return "critical";
        }
        if ("WARNING".equalsIgnoreCase(severity)) {
            return "warning";
        }
        return "neutral";
    }

    private String currency(java.math.BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN"))
                .format(value == null ? java.math.BigDecimal.ZERO : value);
    }
}
