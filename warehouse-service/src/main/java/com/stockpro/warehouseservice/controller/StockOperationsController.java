package com.stockpro.warehouseservice.controller;

import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.service.StockAlertService;
import com.stockpro.warehouseservice.service.StockAuditService;
import com.stockpro.warehouseservice.service.StockBarcodeService;
import com.stockpro.warehouseservice.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock")
@Tag(name = "Stock Operations", description = "Additional APIs for stock operations")
public class StockOperationsController {

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private StockAuditService stockAuditService;

    @Autowired
    private StockBarcodeService stockBarcodeService;

    @Autowired
    private StockAlertService stockAlertService;

    @GetMapping("/movements")
    @Operation(summary = "Get stock movement history")
    public ResponseEntity<List<StockMovementResponseDTO>> getMovementHistory(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId) {
        return ResponseEntity.ok(
                stockMovementService.getMovementHistory(warehouseId, productId));
    }

    @PostMapping("/audit")
    @Operation(summary = "Perform stock audit / cycle count")
    public ResponseEntity<StockAuditResponseDTO> performAudit(
            @Valid @RequestBody StockAuditRequestDTO dto) {
        return ResponseEntity.ok(stockAuditService.performAudit(dto));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Lookup product and stock by barcode")
    public ResponseEntity<BarcodeStockLookupResponseDTO> lookupByBarcode(
            @PathVariable String barcode) {
        return ResponseEntity.ok(stockBarcodeService.lookupByBarcode(barcode));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get active stock alerts")
    public ResponseEntity<List<StockAlertResponseDTO>> getActiveAlerts() {
        return ResponseEntity.ok(stockAlertService.getActiveAlerts());
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge an active stock alert")
    public ResponseEntity<StockAlertResponseDTO> acknowledgeAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody AcknowledgeAlertRequestDTO dto) {
        return ResponseEntity.ok(
                stockAlertService.acknowledgeAlert(alertId, dto.getAcknowledgedBy()));
    }
}
