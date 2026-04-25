package com.stockpro.web.controller;

import com.stockpro.web.dto.request.PurchaseOrderFilterRequest;
import com.stockpro.web.dto.request.PurchaseOrderRequest;
import com.stockpro.web.dto.request.SupplierFormRequest;
import com.stockpro.web.service.PurchaseWebService;
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
@RequestMapping("/purchase")
public class PurchaseController {

    private final PurchaseWebService purchaseWebService;

    public PurchaseController(PurchaseWebService purchaseWebService) {
        this.purchaseWebService = purchaseWebService;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewPurchaseOrders(@ModelAttribute("filters") PurchaseOrderFilterRequest filters, Model model) {
        model.addAttribute("activeMenu", "purchase-orders");
        model.addAttribute("ordersPage", purchaseWebService.getPurchaseOrders(filters));
        model.addAttribute("supplierOptions", purchaseWebService.getSuppliers(null, null, null));
        return "purchase-orders";
    }

    @GetMapping("/orders/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewPODetail(@PathVariable Long poId, Model model) {
        model.addAttribute("activeMenu", "purchase-orders");
        model.addAttribute("detail", purchaseWebService.getPurchaseOrderDetail(poId));
        model.addAttribute("receiveForm", purchaseWebService.getPurchaseOrderForm(poId));
        return "po-detail";
    }

    @GetMapping("/orders/new")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String createPO(Model model) {
        model.addAttribute("activeMenu", "purchase-orders");
        model.addAttribute("pageMode", "create");
        model.addAttribute("poForm", purchaseWebService.newPurchaseOrder(SecurityUtils.currentUserIdOrDefault(1L)));
        model.addAttribute("supplierOptions", purchaseWebService.getSuppliers(null, null, null));
        model.addAttribute("warehouseOptions", purchaseWebService.getWarehouseOptions());
        return "po-form";
    }

    @PostMapping("/orders/new")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String createPOSubmit(@Valid @ModelAttribute("poForm") PurchaseOrderRequest poForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "purchase-orders");
            model.addAttribute("pageMode", "create");
            model.addAttribute("supplierOptions", purchaseWebService.getSuppliers(null, null, null));
            model.addAttribute("warehouseOptions", purchaseWebService.getWarehouseOptions());
            return "po-form";
        }
        purchaseWebService.createPurchaseOrder(poForm);
        redirectAttributes.addFlashAttribute("successMessage", "Purchase order created successfully.");
        return "redirect:/purchase/orders";
    }

    @GetMapping("/orders/{poId}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String editPO(@PathVariable Long poId, Model model) {
        model.addAttribute("activeMenu", "purchase-orders");
        model.addAttribute("pageMode", "edit");
        model.addAttribute("poForm", purchaseWebService.getPurchaseOrderForm(poId));
        model.addAttribute("detail", purchaseWebService.getPurchaseOrderDetail(poId));
        model.addAttribute("supplierOptions", purchaseWebService.getSuppliers(null, null, null));
        model.addAttribute("warehouseOptions", purchaseWebService.getWarehouseOptions());
        return "po-form";
    }

    @PostMapping("/orders/{poId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public String approvePO(@PathVariable Long poId, RedirectAttributes redirectAttributes) {
        purchaseWebService.approvePurchaseOrder(poId);
        redirectAttributes.addFlashAttribute("successMessage", "Purchase order approved.");
        return "redirect:/purchase/orders/" + poId;
    }

    @PostMapping("/orders/{poId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String cancelPO(@PathVariable Long poId, RedirectAttributes redirectAttributes) {
        purchaseWebService.cancelPurchaseOrder(poId);
        redirectAttributes.addFlashAttribute("successMessage", "Purchase order cancelled.");
        return "redirect:/purchase/orders/" + poId;
    }

    @PostMapping("/orders/{poId}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_STAFF')")
    public String receiveGoods(@PathVariable Long poId,
            @ModelAttribute("receiveForm") PurchaseOrderRequest receiveForm,
            RedirectAttributes redirectAttributes) {
        purchaseWebService.receiveGoods(poId, receiveForm.getLineItems());
        redirectAttributes.addFlashAttribute("successMessage", "Goods receipt completed.");
        return "redirect:/purchase/orders/" + poId;
    }

    @GetMapping("/orders/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewPOsBySupplier(@PathVariable Long supplierId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("supplierId", supplierId);
        return "redirect:/purchase/orders";
    }

    @GetMapping("/orders/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewPOsByStatus(@PathVariable String status, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("status", status);
        return "redirect:/purchase/orders";
    }

    @GetMapping("/orders/date-range")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewPOsByDateRange(@RequestParam String fromDate,
            @RequestParam String toDate,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("fromDate", fromDate);
        redirectAttributes.addAttribute("toDate", toDate);
        return "redirect:/purchase/orders";
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewSuppliers(@RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            Model model) {
        model.addAttribute("activeMenu", "suppliers");
        model.addAttribute("search", search);
        model.addAttribute("city", city);
        model.addAttribute("country", country);
        model.addAttribute("suppliers", purchaseWebService.getSuppliers(search, city, country));
        return "suppliers";
    }

    @GetMapping("/suppliers/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER','INVENTORY_MANAGER','WAREHOUSE_STAFF')")
    public String viewSupplierDetail(@PathVariable Long supplierId, Model model) {
        model.addAttribute("activeMenu", "suppliers");
        model.addAttribute("supplier", purchaseWebService.getSupplier(supplierId));
        return "supplier-detail";
    }

    @GetMapping("/suppliers/new")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String addSupplier(Model model) {
        model.addAttribute("activeMenu", "suppliers");
        model.addAttribute("pageMode", "create");
        model.addAttribute("supplierForm", new SupplierFormRequest());
        return "supplier-form";
    }

    @PostMapping("/suppliers/new")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String addSupplierSubmit(@Valid @ModelAttribute("supplierForm") SupplierFormRequest supplierForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "suppliers");
            model.addAttribute("pageMode", "create");
            return "supplier-form";
        }
        purchaseWebService.createSupplier(supplierForm);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier created successfully.");
        return "redirect:/purchase/suppliers";
    }

    @GetMapping("/suppliers/{supplierId}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String editSupplier(@PathVariable Long supplierId, Model model) {
        model.addAttribute("activeMenu", "suppliers");
        model.addAttribute("pageMode", "edit");
        model.addAttribute("supplierForm", purchaseWebService.getSupplierForm(supplierId));
        return "supplier-form";
    }

    @PostMapping("/suppliers/{supplierId}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String editSupplierSubmit(@PathVariable Long supplierId,
            @Valid @ModelAttribute("supplierForm") SupplierFormRequest supplierForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "suppliers");
            model.addAttribute("pageMode", "edit");
            return "supplier-form";
        }
        purchaseWebService.updateSupplier(supplierId, supplierForm);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier updated successfully.");
        return "redirect:/purchase/suppliers/" + supplierId;
    }

    @PostMapping("/suppliers/{supplierId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASE_OFFICER')")
    public String deactivateSupplier(@PathVariable Long supplierId, RedirectAttributes redirectAttributes) {
        purchaseWebService.deactivateSupplier(supplierId);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier deactivated.");
        return "redirect:/purchase/suppliers";
    }
}
