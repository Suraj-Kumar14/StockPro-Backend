package com.stockpro.warehouse.controller;

import com.stockpro.warehouse.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouse.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouse.dto.response.WarehouseResponse;
import com.stockpro.warehouse.dto.response.WarehouseUtilizationResponse;
import com.stockpro.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/warehouses")
@Tag(name = "Warehouse Management", description = "Warehouse master data and utilization APIs.")
@SecurityRequirement(name = "bearerAuth")
public class WarehouseController {

    private static final String CREATE_WAREHOUSE_EXAMPLE = """
            {
              "name": "Central Distribution Hub",
              "location": "Bengaluru",
              "address": "Plot 12, Electronics City Phase 1, Bengaluru",
              "managerId": 101,
              "capacity": 5000,
              "phone": "+91-9876543210"
            }
            """;

    private static final String UPDATE_WAREHOUSE_EXAMPLE = """
            {
              "name": "Central Distribution Hub",
              "location": "Bengaluru",
              "address": "Plot 21, Electronics City Phase 2, Bengaluru",
              "managerId": 102,
              "capacity": 6000,
              "phone": "+91-9988776655",
              "isActive": true
            }
            """;

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create warehouse",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "CreateWarehouse", value = CREATE_WAREHOUSE_EXAMPLE))))
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get warehouse by id")
    public ResponseEntity<WarehouseResponse> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get all warehouses with pagination")
    public ResponseEntity<Page<WarehouseResponse>> getAllWarehouses(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "warehouseId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(warehouseService.getAllWarehouses(page, size, sortBy, sortDir));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Update warehouse",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "UpdateWarehouse", value = UPDATE_WAREHOUSE_EXAMPLE))))
    public ResponseEntity<WarehouseResponse> updateWarehouse(@PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft deactivate warehouse")
    public ResponseEntity<WarehouseResponse> deactivateWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.deactivateWarehouse(id));
    }

    @GetMapping("/{id}/utilization")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get warehouse utilization percentage")
    public ResponseEntity<WarehouseUtilizationResponse> getWarehouseUtilization(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseUtilization(id));
    }
}
