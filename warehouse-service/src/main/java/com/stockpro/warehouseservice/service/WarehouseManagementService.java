package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouseservice.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouseservice.dto.response.WarehouseResponse;
import com.stockpro.warehouseservice.dto.response.WarehouseSummaryResponse;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.events.WarehouseEvent;
import com.stockpro.warehouseservice.exception.CapacityExceededException;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseManagementService {

    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final WarehouseEventPublisher warehouseEventPublisher;

    @Value("${stockpro.rabbitmq.warehouse.routing.warehouse-created}")
    private String warehouseCreatedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.warehouse-updated}")
    private String warehouseUpdatedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.warehouse-activated}")
    private String warehouseActivatedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.warehouse-deactivated}")
    private String warehouseDeactivatedRouting;

    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request, Long actorId) {
        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Warehouse code already exists");
        }
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .code(request.getCode())
                .location(request.getLocation())
                .address(request.getAddress())
                .managerId(request.getManagerId())
                .capacity(request.getCapacity())
                .usedCapacity(0)
                .phone(request.getPhone())
                .isActive(true)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();
        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created successfully: warehouseId={}, code={}", saved.getWarehouseId(), saved.getCode());
        publishWarehouseEvent(warehouseCreatedRouting, "WAREHOUSE_CREATED", saved, actorId);
        return toResponse(saved);
    }

    public WarehouseResponse getWarehouseById(Long warehouseId) {
        return toResponse(getWarehouseEntity(warehouseId));
    }

    public WarehouseResponse getWarehouseByCode(String code) {
        return toResponse(warehouseRepository.findByCode(code)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found with code: " + code)));
    }

    public Page<WarehouseResponse> getAllWarehouses(Boolean isActive, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
        Page<Warehouse> warehouses = isActive == null ? warehouseRepository.findAll(pageable) : warehouseRepository.findByIsActive(isActive, pageable);
        return warehouses.map(this::toResponse);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long warehouseId, UpdateWarehouseRequest request, Long actorId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        int actualUsedCapacity = defaultIfNull(stockLevelRepository.sumQuantityByWarehouseId(warehouseId));
        if (request.getCapacity() < actualUsedCapacity) {
            throw new CapacityExceededException("Warehouse capacity exceeded");
        }
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setAddress(request.getAddress());
        warehouse.setManagerId(request.getManagerId());
        warehouse.setCapacity(request.getCapacity());
        warehouse.setPhone(request.getPhone());
        warehouse.setIsActive(request.getIsActive() == null ? warehouse.getIsActive() : request.getIsActive());
        warehouse.setUsedCapacity(actualUsedCapacity);
        warehouse.setUpdatedBy(actorId);
        Warehouse updated = warehouseRepository.save(warehouse);
        publishWarehouseEvent(warehouseUpdatedRouting, "WAREHOUSE_UPDATED", updated, actorId);
        return toResponse(updated);
    }

    @Transactional
    public WarehouseResponse deactivateWarehouse(Long warehouseId, Long actorId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        warehouse.setIsActive(false);
        warehouse.setUpdatedBy(actorId);
        Warehouse updated = warehouseRepository.save(warehouse);
        publishWarehouseEvent(warehouseDeactivatedRouting, "WAREHOUSE_DEACTIVATED", updated, actorId);
        return toResponse(updated);
    }

    @Transactional
    public WarehouseResponse activateWarehouse(Long warehouseId, Long actorId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        warehouse.setIsActive(true);
        warehouse.setUpdatedBy(actorId);
        Warehouse updated = warehouseRepository.save(warehouse);
        publishWarehouseEvent(warehouseActivatedRouting, "WAREHOUSE_ACTIVATED", updated, actorId);
        return toResponse(updated);
    }

    public List<WarehouseResponse> getWarehousesByManager(Long managerId) {
        return warehouseRepository.findByManagerId(managerId).stream().map(this::toResponse).toList();
    }

    public WarehouseSummaryResponse getWarehouseSummary() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        long total = warehouses.size();
        long active = warehouses.stream().filter(w -> Boolean.TRUE.equals(w.getIsActive())).count();
        long totalCapacity = warehouses.stream().mapToLong(w -> defaultIfNull(w.getCapacity())).sum();
        long usedCapacity = warehouses.stream().mapToLong(w -> defaultIfNull(w.getUsedCapacity())).sum();
        double averageUtilization = warehouses.isEmpty()
                ? 0
                : warehouses.stream().mapToDouble(w -> calculateUtilization(defaultIfNull(w.getUsedCapacity()), defaultIfNull(w.getCapacity()))).average().orElse(0);
        return WarehouseSummaryResponse.builder()
                .totalWarehouses(total)
                .activeWarehouses(active)
                .inactiveWarehouses(total - active)
                .totalCapacity(totalCapacity)
                .usedCapacity(usedCapacity)
                .availableCapacity(totalCapacity - usedCapacity)
                .averageUtilizationPercentage(averageUtilization)
                .build();
    }

    public Warehouse getWarehouseEntity(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findByWarehouseId(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found with ID: " + warehouseId));
        warehouse.setUsedCapacity(defaultIfNull(stockLevelRepository.sumQuantityByWarehouseId(warehouseId)));
        return warehouse;
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        int capacity = defaultIfNull(warehouse.getCapacity());
        int usedCapacity = defaultIfNull(warehouse.getUsedCapacity());
        return WarehouseResponse.builder()
                .warehouseId(warehouse.getWarehouseId())
                .name(warehouse.getName())
                .code(warehouse.getCode())
                .location(warehouse.getLocation())
                .address(warehouse.getAddress())
                .managerId(warehouse.getManagerId())
                .capacity(capacity)
                .usedCapacity(usedCapacity)
                .availableCapacity(Math.max(capacity - usedCapacity, 0))
                .utilizationPercentage(calculateUtilization(usedCapacity, capacity))
                .isActive(warehouse.getIsActive())
                .phone(warehouse.getPhone())
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }

    private void publishWarehouseEvent(String routingKey, String eventType, Warehouse warehouse, Long actorId) {
        warehouseEventPublisher.publishWarehouseEvent(routingKey, WarehouseEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .warehouseId(warehouse.getWarehouseId())
                .warehouseCode(warehouse.getCode())
                .warehouseName(warehouse.getName())
                .managerId(warehouse.getManagerId())
                .capacity(warehouse.getCapacity())
                .usedCapacity(warehouse.getUsedCapacity())
                .isActive(warehouse.getIsActive())
                .actorId(actorId)
                .eventTime(LocalDateTime.now())
                .newValue(toResponse(warehouse))
                .build());
    }

    private double calculateUtilization(int usedCapacity, int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        return Math.round(((double) usedCapacity / capacity) * 10000.0) / 100.0;
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
