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
public class WarehouseApiV1Controller {

    private final WarehouseManagementService warehouseManagementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseManagementService.createWarehouse(request, actorId(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Page<WarehouseResponse> getWarehouses(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return warehouseManagementService.getAllWarehouses(isActive, page, size, sortBy, sortDir);
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
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse updateWarehouse(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseRequest request, Authentication authentication) {
        return warehouseManagementService.updateWarehouse(id, request, actorId(authentication));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse deactivateWarehouse(@PathVariable Long id, Authentication authentication) {
        return warehouseManagementService.deactivateWarehouse(id, actorId(authentication));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse activateWarehouse(@PathVariable Long id, Authentication authentication) {
        return warehouseManagementService.activateWarehouse(id, actorId(authentication));
    }

    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<WarehouseResponse> getWarehousesByManager(@PathVariable Long managerId) {
        return warehouseManagementService.getWarehousesByManager(managerId);
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
