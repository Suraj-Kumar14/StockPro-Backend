package com.stockpro.supplierservice.controller;

import com.stockpro.supplierservice.dto.request.BlacklistSupplierRequest;
import com.stockpro.supplierservice.dto.request.CreateSupplierRequest;
import com.stockpro.supplierservice.dto.request.DeactivateSupplierRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRatingRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRequest;
import com.stockpro.supplierservice.dto.response.SupplierPerformanceResponse;
import com.stockpro.supplierservice.dto.response.SupplierPurchaseValidationResponse;
import com.stockpro.supplierservice.dto.response.SupplierResponse;
import com.stockpro.supplierservice.dto.response.SupplierSummaryResponse;
import com.stockpro.supplierservice.entity.SupplierStatus;
import com.stockpro.supplierservice.security.AuthenticatedUser;
import com.stockpro.supplierservice.service.SupplierManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Management V1", description = "Supplier lifecycle, validation, and analytics APIs")
public class SupplierApiV1Controller {

    private final SupplierManagementService supplierManagementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Operation(summary = "Create supplier")
    public ResponseEntity<SupplierResponse> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplierManagementService.createSupplier(request, user != null ? user.getUserId() : null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Update supplier")
    public SupplierResponse updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return supplierManagementService.updateSupplier(id, request, user != null ? user.getUserId() : null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER','STAFF')")
    @Operation(summary = "Get suppliers with optional active/status filters")
    public Page<SupplierResponse> getSuppliers(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return supplierManagementService.getAllSuppliers(isActive, status, page, size, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER','STAFF')")
    @Operation(summary = "Search suppliers with filters")
    public Page<SupplierResponse> searchSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer maxLeadTimeDays,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return supplierManagementService.searchSuppliers(keyword, status, isActive, city, country, minRating, maxLeadTimeDays, page, size, sortBy, sortDir);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get active suppliers for PO dropdowns")
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierManagementService.getActiveSuppliers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER','STAFF')")
    @Operation(summary = "Get supplier by ID")
    public SupplierResponse getSupplierById(@PathVariable Long id) {
        return supplierManagementService.getSupplierById(id);
    }

    @GetMapping("/code/{supplierCode}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get supplier by code")
    public SupplierResponse getSupplierByCode(@PathVariable String supplierCode) {
        return supplierManagementService.getSupplierByCode(supplierCode);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get supplier by email")
    public SupplierResponse getSupplierByEmail(@PathVariable String email) {
        return supplierManagementService.getSupplierByEmail(email);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Activate supplier")
    public SupplierResponse activateSupplier(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return supplierManagementService.activateSupplier(id, user != null ? user.getUserId() : null);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Deactivate supplier")
    public SupplierResponse deactivateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody DeactivateSupplierRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return supplierManagementService.deactivateSupplier(id, request, user != null ? user.getUserId() : null);
    }

    @PatchMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Blacklist supplier")
    public SupplierResponse blacklistSupplier(
            @PathVariable Long id,
            @Valid @RequestBody BlacklistSupplierRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return supplierManagementService.blacklistSupplier(id, request, user != null ? user.getUserId() : null);
    }

    @PatchMapping("/{id}/rating")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    @Operation(summary = "Update supplier rating")
    public SupplierResponse updateRating(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRatingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return supplierManagementService.updateSupplierRating(id, request, user != null ? user.getUserId() : null);
    }

    @GetMapping("/{id}/performance")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    @Operation(summary = "Get supplier performance")
    public SupplierPerformanceResponse getPerformance(@PathVariable Long id) {
        return supplierManagementService.getSupplierPerformance(id);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    @Operation(summary = "Get supplier summary")
    public SupplierSummaryResponse getSummary() {
        return supplierManagementService.getSupplierSummary();
    }

    @GetMapping("/top-rated")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    @Operation(summary = "Get top rated suppliers")
    public List<SupplierResponse> getTopRatedSuppliers() {
        return supplierManagementService.getTopRatedSuppliers();
    }

    @GetMapping("/{id}/validate-for-purchase")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    @Operation(summary = "Validate supplier for purchase usage")
    public SupplierPurchaseValidationResponse validateForPurchase(
            @Parameter(description = "Supplier ID") @PathVariable Long id) {
        return supplierManagementService.validateSupplierForPurchase(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete supplier if safe")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierManagementService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
