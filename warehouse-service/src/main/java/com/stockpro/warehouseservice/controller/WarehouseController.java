package com.stockpro.warehouseservice.controller;

import com.stockpro.warehouseservice.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouseservice.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouseservice.dto.response.WarehouseResponse;
import com.stockpro.warehouseservice.dto.response.WarehouseSummaryResponse;
import com.stockpro.warehouseservice.security.AuthenticatedUser;
import com.stockpro.warehouseservice.service.WarehouseManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouse APIs")
public class WarehouseController {

    private final WarehouseManagementService warehouseManagementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseManagementService.createWarehouse(request, actorId(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')")
    public Page<WarehouseResponse> getWarehouses(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return warehouseManagementService.getAllWarehouses(isActive, search, status, city, state, page, size, sortBy, sortDir);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')")
    public List<WarehouseResponse> getActiveWarehouses() {
        return warehouseManagementService.getActiveWarehouses();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    public WarehouseResponse getWarehouseById(@PathVariable Long id) {
        return warehouseManagementService.getWarehouseById(id);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WarehouseResponse getWarehouseByCode(@PathVariable String code) {
        return warehouseManagementService.getWarehouseByCode(code);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WarehouseResponse updateWarehouse(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseRequest request, Authentication authentication) {
        return warehouseManagementService.updateWarehouse(id, request, actorId(authentication));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WarehouseResponse deactivateWarehouse(@PathVariable Long id, Authentication authentication) {
        return warehouseManagementService.deactivateWarehouse(id, actorId(authentication));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WarehouseResponse activateWarehouse(@PathVariable Long id, Authentication authentication) {
        return warehouseManagementService.activateWarehouse(id, actorId(authentication));
    }

    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<WarehouseResponse> getWarehousesByManager(@PathVariable Long managerId) {
        return warehouseManagementService.getWarehousesByManager(managerId);
    }

    @PutMapping("/{warehouseId}/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WarehouseResponse assignManager(@PathVariable Long warehouseId, @PathVariable Long managerId, Authentication authentication) {
        return warehouseManagementService.assignManager(warehouseId, managerId, actorId(authentication));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WarehouseSummaryResponse getSummary() {
        return warehouseManagementService.getWarehouseSummary();
    }

    private Long actorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }
}
