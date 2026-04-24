package com.stockpro.warehouse.controller;

import com.stockpro.warehouse.dto.request.ReleaseReservationRequest;
import com.stockpro.warehouse.dto.request.ReserveStockRequest;
import com.stockpro.warehouse.dto.request.StockSearchRequest;
import com.stockpro.warehouse.dto.request.TransferStockRequest;
import com.stockpro.warehouse.dto.request.UpdateStockRequest;
import com.stockpro.warehouse.dto.response.LowStockItemResponse;
import com.stockpro.warehouse.dto.response.StockLevelQuantityResponse;
import com.stockpro.warehouse.dto.response.StockLevelResponse;
import com.stockpro.warehouse.dto.response.TransferStockResponse;
import com.stockpro.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Stock Management", description = "Stock levels, reservations, transfers and low stock lookup APIs.")
@SecurityRequirement(name = "bearerAuth")
public class StockController {

    private static final String UPDATE_STOCK_EXAMPLE = """
            {
              "warehouseId": 1,
              "productId": 501,
              "quantity": 25.0000,
              "unitCost": 149.5000,
              "referenceId": 9001,
              "referenceType": "PURCHASE_ORDER",
              "location": "A-01-RACK-03",
              "notes": "Goods received against PO-9001"
            }
            """;

    private static final String RESERVE_STOCK_EXAMPLE = """
            {
              "warehouseId": 1,
              "productId": 501,
              "quantity": 5.0000,
              "referenceId": 12001,
              "referenceType": "SALES_ORDER",
              "notes": "Reserved for SO-12001"
            }
            """;

    private static final String RELEASE_STOCK_EXAMPLE = """
            {
              "warehouseId": 1,
              "productId": 501,
              "quantity": 2.0000,
              "referenceId": 12001,
              "referenceType": "SALES_ORDER",
              "notes": "Released after order change"
            }
            """;

    private static final String TRANSFER_STOCK_EXAMPLE = """
            {
              "sourceWarehouseId": 1,
              "destinationWarehouseId": 2,
              "productId": 501,
              "quantity": 12.0000,
              "referenceId": 15001,
              "referenceType": "WAREHOUSE_TRANSFER",
              "unitCost": 149.5000,
              "notes": "Redistribution to south fulfillment center"
            }
            """;

    private static final String SEARCH_STOCK_EXAMPLE = """
            {
              "warehouseId": 1,
              "productId": 501,
              "location": "A-01",
              "lowStockOnly": false,
              "page": 0,
              "size": 20,
              "sortBy": "lastUpdated",
              "sortDir": "desc"
            }
            """;

    private final WarehouseService warehouseService;

    public StockController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/api/v1/stock/level")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get stock level by warehouse and product")
    public ResponseEntity<StockLevelResponse> getStockLevel(@RequestParam Long warehouseId, @RequestParam Long productId) {
        return ResponseEntity.ok(warehouseService.getStockLevel(warehouseId, productId));
    }

    @PutMapping("/api/v1/stock/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "Adjust stock quantity",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "UpdateStock", value = UPDATE_STOCK_EXAMPLE))))
    public ResponseEntity<StockLevelResponse> updateStock(@Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(warehouseService.updateStock(request));
    }

    @PostMapping("/api/v1/stock/reserve")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "Reserve available stock",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "ReserveStock", value = RESERVE_STOCK_EXAMPLE))))
    public ResponseEntity<StockLevelResponse> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        return ResponseEntity.ok(warehouseService.reserveStock(request));
    }

    @PostMapping("/api/v1/stock/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "Release reserved stock",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "ReleaseReservation", value = RELEASE_STOCK_EXAMPLE))))
    public ResponseEntity<StockLevelResponse> releaseReservation(
            @Valid @RequestBody ReleaseReservationRequest request) {
        return ResponseEntity.ok(warehouseService.releaseReservation(request));
    }

    @PostMapping("/api/v1/stock/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "Transfer stock atomically between warehouses",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "TransferStock", value = TRANSFER_STOCK_EXAMPLE))))
    public ResponseEntity<TransferStockResponse> transferStock(@Valid @RequestBody TransferStockRequest request) {
        return ResponseEntity.ok(warehouseService.transferStock(request));
    }

    @GetMapping("/api/v1/stock/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get stock by warehouse")
    public ResponseEntity<Page<StockLevelResponse>> getStockByWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "lastUpdated") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(warehouseService.getStockByWarehouse(warehouseId, page, size, sortBy, sortDir));
    }

    @GetMapping("/api/v1/stock/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get stock by product")
    public ResponseEntity<Page<StockLevelResponse>> getStockByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "lastUpdated") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(warehouseService.getStockByProduct(productId, page, size, sortBy, sortDir));
    }

    @GetMapping("/api/v1/stock/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get low stock items for alerting and dashboards")
    public ResponseEntity<Page<LowStockItemResponse>> getLowStockItems(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(warehouseService.getLowStockItems(page, size));
    }

    @PostMapping("/api/v1/stock/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(
            summary = "Advanced stock search",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "SearchStock", value = SEARCH_STOCK_EXAMPLE))))
    public ResponseEntity<Page<StockLevelResponse>> searchStock(@Valid @RequestBody StockSearchRequest request) {
        return ResponseEntity.ok(warehouseService.searchStock(request));
    }

    @GetMapping("/warehouse/internal/low-stock")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Internal low stock endpoint used by alert-service scheduler")
    public ResponseEntity<java.util.List<LowStockItemResponse>> getInternalLowStockItems() {
        return ResponseEntity.ok(warehouseService.getLowStockItemsForAlert());
    }

    @PostMapping("/warehouse/internal/stock-levels")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Internal live stock lookup endpoint used by product-service")
    public ResponseEntity<java.util.List<StockLevelQuantityResponse>> getStockLevels(@RequestBody java.util.List<Long> productIds) {
        return ResponseEntity.ok(warehouseService.getStockLevels(productIds));
    }
}
