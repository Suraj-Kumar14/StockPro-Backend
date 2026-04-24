package com.stockpro.movement.controller;

import com.stockpro.movement.dto.request.MovementSearchRequest;
import com.stockpro.movement.dto.request.RecordMovementRequest;
import com.stockpro.movement.dto.response.MovementResponse;
import com.stockpro.movement.dto.response.StockInOutSummaryResponse;
import com.stockpro.movement.enums.MovementType;
import com.stockpro.movement.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/movements")
@Tag(name = "Stock Movement Management", description = "Immutable stock movement audit trail APIs.")
@SecurityRequirement(name = "bearerAuth")
public class MovementController {

    private static final String RECORD_MOVEMENT_EXAMPLE = """
            {
              "productId": 101,
              "warehouseId": 5,
              "movementType": "STOCK_IN",
              "quantity": 25.0000,
              "referenceId": 9001,
              "referenceType": "PURCHASE_ORDER",
              "unitCost": 149.5000,
              "performedBy": 12,
              "notes": "Goods received against PO-9001",
              "movementDate": "2026-04-23T10:30:00",
              "balanceAfter": 250.0000
            }
            """;

    private static final String SEARCH_MOVEMENT_EXAMPLE = """
            {
              "productId": 101,
              "warehouseId": 5,
              "movementType": "STOCK_IN",
              "startDate": "2026-04-01T00:00:00",
              "endDate": "2026-04-30T23:59:59",
              "page": 0,
              "size": 20,
              "sortBy": "movementDate",
              "sortDir": "desc"
            }
            """;

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(
            summary = "Record a new stock movement",
            description = "Creates a write-once movement record. Existing movement records cannot be updated or deleted.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "RecordMovement", value = RECORD_MOVEMENT_EXAMPLE))))
    public ResponseEntity<MovementResponse> recordMovement(
            @Valid @org.springframework.web.bind.annotation.RequestBody RecordMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movementService.recordMovement(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(summary = "Get all stock movements with pagination")
    public ResponseEntity<Page<MovementResponse>> getAllMovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "movementDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(movementService.getAllMovements(page, size, sortBy, sortDir));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(summary = "Get movements by product")
    public ResponseEntity<List<MovementResponse>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(movementService.getByProduct(productId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'MANAGER')")
    @Operation(summary = "Get movements by warehouse")
    public ResponseEntity<List<MovementResponse>> getByWarehouse(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(movementService.getByWarehouse(warehouseId));
    }

    @GetMapping("/type/{movementType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(summary = "Get movements by movement type")
    public ResponseEntity<List<MovementResponse>> getByType(@PathVariable MovementType movementType) {
        return ResponseEntity.ok(movementService.getByType(movementType));
    }

    @GetMapping("/reference/{referenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(summary = "Get movements by reference id")
    public ResponseEntity<List<MovementResponse>> getByReference(@PathVariable Long referenceId) {
        return ResponseEntity.ok(movementService.getByReference(referenceId));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(summary = "Get movements by movement date range")
    public ResponseEntity<List<MovementResponse>> getByDateRange(
            @Parameter(example = "2026-04-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(example = "2026-04-30T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(movementService.getByDateRange(startDate, endDate));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(summary = "Get movement history by product and warehouse ordered by movementDate ascending")
    public ResponseEntity<List<MovementResponse>> getMovementHistory(
            @RequestParam Long productId,
            @RequestParam Long warehouseId) {
        return ResponseEntity.ok(movementService.getMovementHistory(productId, warehouseId));
    }

    @GetMapping("/stock-in/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(summary = "Get stock-in summary for a product")
    public ResponseEntity<StockInOutSummaryResponse> getStockIn(@PathVariable Long productId) {
        return ResponseEntity.ok(StockInOutSummaryResponse.builder()
                .productId(productId)
                .totalStockIn(movementService.getStockIn(productId))
                .totalStockOut(movementService.getStockOut(productId))
                .build());
    }

    @GetMapping("/stock-out/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(summary = "Get stock-out summary for a product")
    public ResponseEntity<StockInOutSummaryResponse> getStockOut(@PathVariable Long productId) {
        return ResponseEntity.ok(StockInOutSummaryResponse.builder()
                .productId(productId)
                .totalStockIn(movementService.getStockIn(productId))
                .totalStockOut(movementService.getStockOut(productId))
                .build());
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'MANAGER')")
    @Operation(
            summary = "Advanced movement search",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "SearchMovements", value = SEARCH_MOVEMENT_EXAMPLE))))
    public ResponseEntity<Page<MovementResponse>> searchMovements(
            @Valid @org.springframework.web.bind.annotation.RequestBody MovementSearchRequest request) {
        return ResponseEntity.ok(movementService.searchMovements(request));
    }
}
