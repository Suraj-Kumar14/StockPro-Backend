package com.stockpro.warehouseservice.controller;

import com.stockpro.warehouseservice.dto.request.AdjustStockRequest;
import com.stockpro.warehouseservice.dto.request.CreateStockLevelRequest;
import com.stockpro.warehouseservice.dto.request.ReleaseReservationRequest;
import com.stockpro.warehouseservice.dto.request.ReserveStockRequest;
import com.stockpro.warehouseservice.dto.request.StockIssueRequest;
import com.stockpro.warehouseservice.dto.request.StockReceiptRequest;
import com.stockpro.warehouseservice.dto.request.TransferStockRequest;
import com.stockpro.warehouseservice.dto.request.UpdateStockRequest;
import com.stockpro.warehouseservice.dto.response.StockLevelResponse;
import com.stockpro.warehouseservice.dto.response.StockSummaryResponse;
import com.stockpro.warehouseservice.dto.response.TransferStockResponse;
import com.stockpro.warehouseservice.security.AuthenticatedUser;
import com.stockpro.warehouseservice.service.StockManagementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockApiV1Controller {

    private final StockManagementService stockManagementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<StockLevelResponse> createStockLevel(@Valid @RequestBody CreateStockLevelRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockManagementService.createStockLevel(request, actorId(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    public Page<StockLevelResponse> searchStock(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) Boolean lowStockOnly,
            @RequestParam(required = false) Boolean overstockOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return stockManagementService.searchStock(warehouseId, productId, locationCode, lowStockOnly, overstockOnly, page, size);
    }

    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    public Page<StockLevelResponse> getStockByWarehouse(@PathVariable Long warehouseId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return stockManagementService.getStockByWarehouse(warehouseId, page, size);
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    public Page<StockLevelResponse> getStockByProduct(@PathVariable Long productId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return stockManagementService.getStockByProduct(productId, page, size);
    }

    @GetMapping("/warehouse/{warehouseId}/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    public StockLevelResponse getStockLevel(@PathVariable Long warehouseId, @PathVariable Long productId) {
        return stockManagementService.getStockLevel(warehouseId, productId);
    }

    @PostMapping("/receive")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public StockLevelResponse receiveStock(@Valid @RequestBody StockReceiptRequest request, Authentication authentication) {
        return stockManagementService.receiveStock(request, actorId(authentication));
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public StockLevelResponse issueStock(@Valid @RequestBody StockIssueRequest request, Authentication authentication) {
        return stockManagementService.issueStock(request, actorId(authentication));
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public StockLevelResponse reserveStock(@Valid @RequestBody ReserveStockRequest request, Authentication authentication) {
        return stockManagementService.reserveStock(request, actorId(authentication));
    }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public StockLevelResponse releaseReservation(@Valid @RequestBody ReleaseReservationRequest request, Authentication authentication) {
        return stockManagementService.releaseReservation(request, actorId(authentication));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public TransferStockResponse transferStock(@Valid @RequestBody TransferStockRequest request, Authentication authentication) {
        return stockManagementService.transferStock(request, actorId(authentication));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public StockLevelResponse adjustStock(@Valid @RequestBody AdjustStockRequest request, Authentication authentication) {
        return stockManagementService.adjustStock(request, actorId(authentication));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public StockLevelResponse updateStock(@Valid @RequestBody UpdateStockRequest request, Authentication authentication) {
        return stockManagementService.updateStock(request, actorId(authentication));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<StockLevelResponse> getLowStock() {
        return stockManagementService.getLowStockItems();
    }

    @GetMapping("/overstock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<StockLevelResponse> getOverstock() {
        return stockManagementService.getOverstockItems();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public StockSummaryResponse getSummary() {
        return stockManagementService.getStockSummary();
    }

    private Long actorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }
}
