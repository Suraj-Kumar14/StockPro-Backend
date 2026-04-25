package com.stockpro.web.controller;

import com.stockpro.web.dto.ReportFormat;
import com.stockpro.web.dto.ReportType;
import com.stockpro.web.dto.request.GenerateReportRequest;
import com.stockpro.web.dto.request.PlatformAlertRequest;
import com.stockpro.web.dto.request.RegisterUserRequest;
import com.stockpro.web.dto.request.ReportFilterRequest;
import com.stockpro.web.dto.request.UpdateUserRequest;
import com.stockpro.web.dto.request.WarehouseFormRequest;
import com.stockpro.web.service.AdminWebService;
import com.stockpro.web.service.PurchaseWebService;
import com.stockpro.web.util.SecurityUtils;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminWebService adminWebService;
    private final PurchaseWebService purchaseWebService;

    public AdminController(AdminWebService adminWebService, PurchaseWebService purchaseWebService) {
        this.adminWebService = adminWebService;
        this.purchaseWebService = purchaseWebService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("activeMenu", "admin-dashboard");
        model.addAttribute("dashboard", adminWebService.getAdminDashboard());
        return "admin-dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("activeMenu", "users");
        model.addAttribute("users", adminWebService.getUsers());
        return "users";
    }

    @GetMapping("/users/new")
    public String addUser(Model model) {
        model.addAttribute("activeMenu", "users");
        model.addAttribute("pageMode", "create");
        model.addAttribute("userForm", new RegisterUserRequest());
        return "user-form";
    }

    @PostMapping("/users/new")
    public String addUserSubmit(@Valid @ModelAttribute("userForm") RegisterUserRequest userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "users");
            model.addAttribute("pageMode", "create");
            return "user-form";
        }
        String responseMessage = adminWebService.addUser(userForm);
        redirectAttributes.addFlashAttribute("successMessage", responseMessage);
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{userId}/edit")
    public String editUser(@PathVariable Long userId, Model model) {
        model.addAttribute("activeMenu", "users");
        model.addAttribute("pageMode", "edit");
        model.addAttribute("user", adminWebService.getUser(userId));
        model.addAttribute("userForm", new UpdateUserRequest(
                adminWebService.getUser(userId).getFullName(),
                adminWebService.getUser(userId).getEmail(),
                adminWebService.getUser(userId).getPhone()));
        return "user-form";
    }

    @PostMapping("/users/{userId}/edit")
    public String editUserSubmit(@PathVariable Long userId,
            @Valid @ModelAttribute("userForm") UpdateUserRequest userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "users");
            model.addAttribute("pageMode", "edit");
            model.addAttribute("user", adminWebService.getUser(userId));
            return "user-form";
        }
        adminWebService.updateUser(userId, userForm);
        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/deactivate")
    public String deactivateUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        adminWebService.deactivateUser(userId);
        redirectAttributes.addFlashAttribute("successMessage", "User deactivated.");
        return "redirect:/admin/users";
    }

    @GetMapping("/warehouses")
    public String manageWarehouses(Model model) {
        model.addAttribute("activeMenu", "warehouses-admin");
        model.addAttribute("warehousesPage", adminWebService.getWarehouses(0, 20));
        return "warehouses-admin";
    }

    @GetMapping("/warehouses/new")
    public String addWarehouse(Model model) {
        model.addAttribute("activeMenu", "warehouses-admin");
        model.addAttribute("pageMode", "create");
        model.addAttribute("warehouseForm", new WarehouseFormRequest());
        return "warehouse-form";
    }

    @PostMapping("/warehouses/new")
    public String addWarehouseSubmit(@Valid @ModelAttribute("warehouseForm") WarehouseFormRequest warehouseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "warehouses-admin");
            model.addAttribute("pageMode", "create");
            return "warehouse-form";
        }
        adminWebService.addWarehouse(warehouseForm);
        redirectAttributes.addFlashAttribute("successMessage", "Warehouse added successfully.");
        return "redirect:/admin/warehouses";
    }

    @GetMapping("/warehouses/{warehouseId}/edit")
    public String editWarehouse(@PathVariable Long warehouseId, Model model) {
        model.addAttribute("activeMenu", "warehouses-admin");
        model.addAttribute("pageMode", "edit");
        model.addAttribute("warehouseForm", adminWebService.getWarehouseForm(warehouseId));
        return "warehouse-form";
    }

    @PostMapping("/warehouses/{warehouseId}/edit")
    public String editWarehouseSubmit(@PathVariable Long warehouseId,
            @Valid @ModelAttribute("warehouseForm") WarehouseFormRequest warehouseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "warehouses-admin");
            model.addAttribute("pageMode", "edit");
            return "warehouse-form";
        }
        adminWebService.updateWarehouse(warehouseId, warehouseForm);
        redirectAttributes.addFlashAttribute("successMessage", "Warehouse updated successfully.");
        return "redirect:/admin/warehouses";
    }

    @PostMapping("/warehouses/{warehouseId}/deactivate")
    public String deactivateWarehouse(@PathVariable Long warehouseId, RedirectAttributes redirectAttributes) {
        adminWebService.deactivateWarehouse(warehouseId);
        redirectAttributes.addFlashAttribute("successMessage", "Warehouse deactivated.");
        return "redirect:/admin/warehouses";
    }

    @GetMapping("/reports")
    public String reportsDashboard(@ModelAttribute("reportFilters") ReportFilterRequest reportFilters, Model model) {
        model.addAttribute("activeMenu", "reports-admin");
        model.addAttribute("totalStockValue", adminWebService.getTotalStockValue(LocalDate.now()));
        model.addAttribute("turnoverRows", adminWebService.getInventoryTurnover(reportFilters));
        model.addAttribute("topMovingPage", adminWebService.getTopMovingProducts(reportFilters));
        model.addAttribute("deadStockPage", adminWebService.getDeadStock(reportFilters));
        model.addAttribute("warehouseOptions", adminWebService.getWarehouses(0, 100).items());
        return "reports-admin";
    }

    @GetMapping("/reports/total-stock-value")
    public String viewTotalStockValue(Model model) {
        model.addAttribute("activeMenu", "reports-admin");
        model.addAttribute("totalStockValue", adminWebService.getTotalStockValue(LocalDate.now()));
        return "reports-admin";
    }

    @GetMapping("/reports/turnover")
    public String viewInventoryTurnover(@ModelAttribute("reportFilters") ReportFilterRequest reportFilters, Model model) {
        model.addAttribute("activeMenu", "reports-admin");
        model.addAttribute("turnoverRows", adminWebService.getInventoryTurnover(reportFilters));
        return "reports-admin";
    }

    @GetMapping("/reports/top-moving")
    public String viewTopMovingProducts(@ModelAttribute("reportFilters") ReportFilterRequest reportFilters, Model model) {
        model.addAttribute("activeMenu", "reports-admin");
        model.addAttribute("reportPage", adminWebService.getTopMovingProducts(reportFilters));
        return "top-moving-report";
    }

    @GetMapping("/reports/dead-stock")
    public String viewDeadStock(@ModelAttribute("reportFilters") ReportFilterRequest reportFilters, Model model) {
        model.addAttribute("activeMenu", "reports-admin");
        model.addAttribute("reportPage", adminWebService.getDeadStock(reportFilters));
        return "dead-stock-report";
    }

    @GetMapping("/reports/generate")
    public String generateInventoryReport(Model model) {
        model.addAttribute("activeMenu", "reports-admin");
        model.addAttribute("reportForm", new GenerateReportRequest(
                ReportType.LOW_STOCK,
                null,
                null,
                null,
                null,
                null,
                ReportFormat.CSV,
                SecurityUtils.currentUserEmailOrDefault("admin@stockpro.local")));
        model.addAttribute("warehouseOptions", adminWebService.getWarehouses(0, 100).items());
        model.addAttribute("supplierOptions", purchaseWebService.getSuppliers(null, null, null));
        return "generate-report";
    }

    @PostMapping("/reports/generate")
    public String generateInventoryReportSubmit(
            @Valid @ModelAttribute("reportForm") GenerateReportRequest reportForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "reports-admin");
            model.addAttribute("warehouseOptions", adminWebService.getWarehouses(0, 100).items());
            model.addAttribute("supplierOptions", purchaseWebService.getSuppliers(null, null, null));
            return "generate-report";
        }
        adminWebService.generateInventoryReport(reportForm);
        redirectAttributes.addFlashAttribute("successMessage", "Inventory report generation requested.");
        return "redirect:/admin/reports";
    }

    @GetMapping("/alerts/send")
    public String sendPlatformAlert(Model model) {
        model.addAttribute("activeMenu", "alerts-admin");
        model.addAttribute("alertForm", new PlatformAlertRequest());
        model.addAttribute("users", adminWebService.getUsers());
        return "send-alert";
    }

    @PostMapping("/alerts/send")
    public String sendPlatformAlertSubmit(@Valid @ModelAttribute("alertForm") PlatformAlertRequest alertForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "alerts-admin");
            model.addAttribute("users", adminWebService.getUsers());
            return "send-alert";
        }
        adminWebService.sendPlatformAlert(alertForm);
        redirectAttributes.addFlashAttribute("successMessage", "Platform alert sent.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/audit-logs")
    public String viewAuditLogs(Model model) {
        model.addAttribute("activeMenu", "audit-logs");
        model.addAttribute("auditEntries", adminWebService.getAuditLogs());
        return "audit-logs";
    }
}
