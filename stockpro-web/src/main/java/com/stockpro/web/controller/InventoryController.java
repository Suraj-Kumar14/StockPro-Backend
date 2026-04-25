package com.stockpro.web.controller;

import com.stockpro.web.dto.request.AlertSearchRequest;
import com.stockpro.web.dto.request.MovementSearchRequest;
import com.stockpro.web.dto.request.ProductFormRequest;
import com.stockpro.web.dto.request.ProductSearchRequest;
import com.stockpro.web.dto.request.ReportFilterRequest;
import com.stockpro.web.dto.request.StockSearchRequest;
import com.stockpro.web.dto.request.TransferStockRequest;
import com.stockpro.web.security.RoleConstants;
import com.stockpro.web.service.InventoryWebService;
import com.stockpro.web.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryWebService inventoryWebService;

    public InventoryController(InventoryWebService inventoryWebService) {
        this.inventoryWebService = inventoryWebService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String dashboard(Model model) {
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("dashboard", inventoryWebService.getDashboard());
        return "dashboard";
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewProducts(@ModelAttribute("filters") ProductSearchRequest filters,
            @RequestParam(required = false) String query,
            Model model) {
        if (query != null && !query.isBlank()) {
            filters.setName(query);
        }
        model.addAttribute("activeMenu", "products");
        model.addAttribute("productsPage", inventoryWebService.getProducts(filters));
        return "products";
    }

    @GetMapping("/products/search")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String searchProducts(@RequestParam String query, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("query", query);
        return "redirect:/inventory/products";
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewProductDetail(@PathVariable Long productId, Model model) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("detail", inventoryWebService.getProductDetail(productId));
        return "product-detail";
    }

    @GetMapping("/products/new")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String addProduct(Model model) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("pageMode", "create");
        model.addAttribute("productForm", new ProductFormRequest());
        return "product-form";
    }

    @PostMapping("/products/new")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String addProductSubmit(@Valid @ModelAttribute("productForm") ProductFormRequest productForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "products");
            model.addAttribute("pageMode", "create");
            return "product-form";
        }
        inventoryWebService.createProduct(productForm);
        redirectAttributes.addFlashAttribute("successMessage", "Product created successfully.");
        return "redirect:/inventory/products";
    }

    @GetMapping("/products/{productId}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String editProduct(@PathVariable Long productId, Model model) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("pageMode", "edit");
        model.addAttribute("productForm", inventoryWebService.getProductForm(productId));
        return "product-form";
    }

    @PostMapping("/products/{productId}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String editProductSubmit(@PathVariable Long productId,
            @Valid @ModelAttribute("productForm") ProductFormRequest productForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "products");
            model.addAttribute("pageMode", "edit");
            return "product-form";
        }
        inventoryWebService.updateProduct(productId, productForm);
        redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully.");
        return "redirect:/inventory/products/" + productId;
    }

    @PostMapping("/products/{productId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String deactivateProduct(@PathVariable Long productId, RedirectAttributes redirectAttributes) {
        inventoryWebService.deactivateProduct(productId);
        redirectAttributes.addFlashAttribute("successMessage", "Product deactivated successfully.");
        return "redirect:/inventory/products";
    }

    @GetMapping("/products/barcode/{barcode}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String scanBarcode(@PathVariable String barcode) {
        Long productId = inventoryWebService.scanBarcode(barcode).getProductId();
        return "redirect:/inventory/products/" + productId;
    }

    @GetMapping("/warehouses")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewWarehouses(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("activeMenu", "warehouses");
        model.addAttribute("warehousesPage", inventoryWebService.getWarehouses(page, size));
        return "warehouses";
    }

    @GetMapping("/warehouses/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewWarehouseDetail(@PathVariable Long warehouseId, Model model) {
        model.addAttribute("activeMenu", "warehouses");
        model.addAttribute("detail", inventoryWebService.getWarehouseDetail(warehouseId));
        return "warehouse-detail";
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewStockLevel(@ModelAttribute("filters") StockSearchRequest filters, Model model) {
        model.addAttribute("activeMenu", "stock");
        model.addAttribute("stockPage", inventoryWebService.getStockLevels(filters));
        model.addAttribute("productOptions", inventoryWebService.getProductOptions());
        model.addAttribute("warehouseOptions", inventoryWebService.getWarehouseOptions());
        return "stock-level";
    }

    @GetMapping("/stock/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String transferStock(Model model) {
        model.addAttribute("activeMenu", "movements");
        model.addAttribute("transferForm", new TransferStockRequest());
        model.addAttribute("productOptions", inventoryWebService.getProductOptions());
        model.addAttribute("warehouseOptions", inventoryWebService.getWarehouseOptions());
        return "transfer-stock";
    }

    @PostMapping("/stock/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String transferStockSubmit(@Valid @ModelAttribute("transferForm") TransferStockRequest transferForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "movements");
            model.addAttribute("productOptions", inventoryWebService.getProductOptions());
            model.addAttribute("warehouseOptions", inventoryWebService.getWarehouseOptions());
            return "transfer-stock";
        }
        inventoryWebService.transferStock(transferForm);
        redirectAttributes.addFlashAttribute("successMessage", "Stock transfer submitted successfully.");
        return "redirect:/inventory/movements";
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewMovements(@ModelAttribute("filters") MovementSearchRequest filters, Model model) {
        model.addAttribute("activeMenu", "movements");
        model.addAttribute("movementsPage", inventoryWebService.getMovements(filters));
        model.addAttribute("productOptions", inventoryWebService.getProductOptions());
        model.addAttribute("warehouseOptions", inventoryWebService.getWarehouseOptions());
        return "movements";
    }

    @GetMapping("/movements/date-range")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewMovementsByDate(@ModelAttribute("filters") MovementSearchRequest filters,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("infoMessage", "Date filter applied.");
        redirectAttributes.addAttribute("startDate", filters.getStartDate());
        redirectAttributes.addAttribute("endDate", filters.getEndDate());
        return "redirect:/inventory/movements";
    }

    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public String viewAlerts(@ModelAttribute("filters") AlertSearchRequest filters, Model model) {
        Long currentUserId = SecurityUtils.currentUserIdOrDefault(null);
        boolean admin = SecurityUtils.hasRole(RoleConstants.ADMIN);
        model.addAttribute("activeMenu", "alerts");
        model.addAttribute("alertsPage", inventoryWebService.getAlerts(filters, currentUserId, admin));
        return "alerts";
    }

    @PostMapping("/alerts/{alertId}/read")
    @PreAuthorize("isAuthenticated()")
    public String markAlertRead(@PathVariable Long alertId, RedirectAttributes redirectAttributes) {
        inventoryWebService.markAlertRead(alertId);
        redirectAttributes.addFlashAttribute("successMessage", "Alert marked as read.");
        return "redirect:/inventory/alerts";
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    @PreAuthorize("isAuthenticated()")
    public String acknowledgeAlert(@PathVariable Long alertId, RedirectAttributes redirectAttributes) {
        inventoryWebService.acknowledgeAlert(alertId);
        redirectAttributes.addFlashAttribute("successMessage", "Alert acknowledged.");
        return "redirect:/inventory/alerts";
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String viewReports(Model model) {
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("reports", inventoryWebService.getReportsDashboard());
        return "reports";
    }

    @GetMapping("/reports/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String viewLowStockReport(@ModelAttribute("filters") ReportFilterRequest filters, Model model) {
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("reportPage", inventoryWebService.getLowStockReport(filters));
        model.addAttribute("productOptions", inventoryWebService.getProductOptions());
        model.addAttribute("warehouseOptions", inventoryWebService.getWarehouseOptions());
        return "low-stock-report";
    }

    @GetMapping("/reports/top-moving")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String viewTopMovingProducts(@ModelAttribute("filters") ReportFilterRequest filters, Model model) {
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("reportPage", inventoryWebService.getTopMovingProducts(filters));
        model.addAttribute("productOptions", inventoryWebService.getProductOptions());
        model.addAttribute("warehouseOptions", inventoryWebService.getWarehouseOptions());
        return "top-moving-report";
    }

    @GetMapping("/reports/dead-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String viewDeadStock(@ModelAttribute("filters") ReportFilterRequest filters, Model model) {
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("reportPage", inventoryWebService.getDeadStockReport(filters));
        model.addAttribute("productOptions", inventoryWebService.getProductOptions());
        model.addAttribute("warehouseOptions", inventoryWebService.getWarehouseOptions());
        return "dead-stock-report";
    }
}
