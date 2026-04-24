package com.stockpro.supplier.controller;

import com.stockpro.supplier.dto.SupplierRatingRequest;
import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Supplier Management", description = "Supplier registry, geo-search, and vendor performance APIs.")
@SecurityRequirement(name = "bearerAuth")
public class SupplierResource {

    private final SupplierService supplierService;

    public SupplierResource(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PURCHASE_OFFICER')")
    @Operation(summary = "Create supplier")
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.createSupplier(supplier));
    }

    @GetMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'PURCHASE_OFFICER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get supplier by id")
    public ResponseEntity<Supplier> getById(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.getById(supplierId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'PURCHASE_OFFICER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get all suppliers")
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'PURCHASE_OFFICER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Search suppliers by name")
    public ResponseEntity<List<Supplier>> searchSuppliers(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(supplierService.searchSuppliers(name));
    }

    @GetMapping("/city/{city}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'PURCHASE_OFFICER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get suppliers by city")
    public ResponseEntity<List<Supplier>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(supplierService.getByCity(city));
    }

    @GetMapping("/country/{country}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'PURCHASE_OFFICER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get suppliers by country")
    public ResponseEntity<List<Supplier>> getByCountry(@PathVariable String country) {
        return ResponseEntity.ok(supplierService.getByCountry(country));
    }

    @PutMapping("/{supplierId}")
    @PreAuthorize("hasRole('PURCHASE_OFFICER')")
    @Operation(summary = "Update supplier profile")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable Long supplierId,
            @RequestBody Supplier supplier) {
        return ResponseEntity.ok(supplierService.updateSupplier(supplierId, supplier));
    }

    @PutMapping("/{supplierId}/deactivate")
    @PreAuthorize("hasRole('PURCHASE_OFFICER')")
    @Operation(summary = "Soft deactivate supplier")
    public ResponseEntity<Supplier> deactivateSupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.deactivateSupplier(supplierId));
    }

    @PutMapping("/{supplierId}/rating")
    @PreAuthorize("hasRole('PURCHASE_OFFICER')")
    @Operation(summary = "Update supplier rating")
    public ResponseEntity<Supplier> updateRating(@PathVariable Long supplierId,
            @RequestBody SupplierRatingRequest request) {
        return ResponseEntity.ok(supplierService.updateRating(supplierId, request.getNewRating()));
    }

    @PutMapping("/internal/{supplierId}/rating")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Internal endpoint used by purchase-service to update supplier performance rating")
    public ResponseEntity<Supplier> updateRatingInternal(@PathVariable Long supplierId,
            @RequestBody SupplierRatingRequest request) {
        return ResponseEntity.ok(supplierService.updateRating(supplierId, request.getNewRating()));
    }
}
