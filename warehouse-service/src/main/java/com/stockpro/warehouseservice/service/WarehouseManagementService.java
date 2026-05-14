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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseManagementService {
    private static final Map<String, String> SORT_FIELD_ALIASES = Map.ofEntries(
            Map.entry("warehouseid", "warehouseId"),
            Map.entry("id", "warehouseId"),
            Map.entry("name", "name"),
            Map.entry("warehousename", "name"),
            Map.entry("code", "code"),
            Map.entry("warehousecode", "code"),
            Map.entry("location", "location"),
            Map.entry("city", "city"),
            Map.entry("state", "state"),
            Map.entry("country", "country"),
            Map.entry("managerid", "managerId"),
            Map.entry("capacity", "capacity"),
            Map.entry("usedcapacity", "usedCapacity"),
            Map.entry("isactive", "isActive"),
            Map.entry("createdat", "createdAt"),
            Map.entry("updatedat", "updatedAt"));

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
        String code = normalizeRequired(request.getCode()).toUpperCase(Locale.ROOT);
        if (warehouseRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Warehouse code already exists");
        }
        Warehouse warehouse = Warehouse.builder()
                .name(normalizeRequired(request.getName()))
                .code(code)
                .location(normalizeRequired(request.getLocation()))
                .address(normalizeOptional(request.getAddress()))
                .city(normalizeRequired(request.getCity()))
                .state(normalizeRequired(request.getState()))
                .country(normalizeRequired(request.getCountry()))
                .managerId(request.getManagerId())
                .capacity(request.getCapacity())
                .usedCapacity(0)
                .phone(normalizeOptional(request.getPhone()))
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
        return getAllWarehouses(isActive, null, null, null, null, page, size, sortBy, sortDir);
    }

    public Page<WarehouseResponse> getAllWarehouses(Boolean isActive, String search, String status, String city, String state, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(resolveSortDirection(sortDir), resolveSortField(sortBy)));
        Specification<Warehouse> filters = warehouseFilters(isActive, search, status, city, state);
        Page<Warehouse> warehouses = filters == null ? warehouseRepository.findAll(pageable) : warehouseRepository.findAll(filters, pageable);
        return warehouses.map(this::toResponse);
    }

    public List<WarehouseResponse> getActiveWarehouses() {
        return warehouseRepository.findByIsActive(true).stream().map(this::toResponse).toList();
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long warehouseId, UpdateWarehouseRequest request, Long actorId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        int actualUsedCapacity = defaultIfNull(stockLevelRepository.sumQuantityByWarehouseId(warehouseId));
        if (request.getCapacity() < actualUsedCapacity) {
            throw new CapacityExceededException("Warehouse capacity exceeded");
        }
        String code = normalizeRequired(request.getCode()).toUpperCase(Locale.ROOT);
        if (!code.equalsIgnoreCase(warehouse.getCode()) && warehouseRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Warehouse code already exists");
        }
        warehouse.setName(normalizeRequired(request.getName()));
        warehouse.setCode(code);
        warehouse.setLocation(normalizeRequired(request.getLocation()));
        warehouse.setAddress(normalizeOptional(request.getAddress()));
        warehouse.setCity(normalizeRequired(request.getCity()));
        warehouse.setState(normalizeRequired(request.getState()));
        warehouse.setCountry(normalizeRequired(request.getCountry()));
        warehouse.setManagerId(request.getManagerId());
        warehouse.setCapacity(request.getCapacity());
        warehouse.setPhone(normalizeOptional(request.getPhone()));
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

    @Transactional
    public WarehouseResponse assignManager(Long warehouseId, Long managerId, Long actorId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        warehouse.setManagerId(managerId);
        warehouse.setUpdatedBy(actorId);
        Warehouse updated = warehouseRepository.save(warehouse);
        publishWarehouseEvent(warehouseUpdatedRouting, "WAREHOUSE_UPDATED", updated, actorId);
        return toResponse(updated);
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
                .city(warehouse.getCity())
                .state(warehouse.getState())
                .country(warehouse.getCountry())
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

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Sort.Direction resolveSortDirection(String sortDir) {
        return "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

    private String resolveSortField(String sortBy) {
        String requestedField = sortBy == null || sortBy.isBlank() ? "name" : sortBy.trim();
        String normalizedField = requestedField.toLowerCase(Locale.ROOT).replace("_", "");
        String resolvedField = SORT_FIELD_ALIASES.get(normalizedField);

        if (resolvedField == null) {
            throw new IllegalArgumentException(
                    "Invalid sortBy '" + requestedField + "'. Allowed values: warehouseId, name, code, location, city, state, country, managerId, capacity, usedCapacity, isActive, createdAt, updatedAt");
        }

        return resolvedField;
    }

    private Specification<Warehouse> warehouseFilters(Boolean isActive, String search, String status, String city, String state) {
        Specification<Warehouse> spec = null;
        Boolean activeFilter = isActive != null ? isActive : statusToActive(status);

        if (activeFilter != null) {
            spec = and(spec, (root, query, cb) -> cb.equal(root.get("isActive"), activeFilter));
        }

        String searchTerm = normalizeOptional(search);
        if (searchTerm != null) {
            String pattern = "%" + searchTerm.toLowerCase(Locale.ROOT) + "%";
            spec = and(spec, (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("location")), pattern),
                    cb.like(cb.lower(root.get("address")), pattern),
                    cb.like(cb.lower(root.get("city")), pattern),
                    cb.like(cb.lower(root.get("state")), pattern),
                    cb.like(cb.lower(root.get("country")), pattern)));
        }

        String cityFilter = normalizeOptional(city);
        if (cityFilter != null) {
            spec = and(spec, (root, query, cb) -> cb.equal(cb.lower(root.get("city")), cityFilter.toLowerCase(Locale.ROOT)));
        }

        String stateFilter = normalizeOptional(state);
        if (stateFilter != null) {
            spec = and(spec, (root, query, cb) -> cb.equal(cb.lower(root.get("state")), stateFilter.toLowerCase(Locale.ROOT)));
        }

        return spec;
    }

    private Specification<Warehouse> and(Specification<Warehouse> current, Specification<Warehouse> next) {
        return current == null ? next : current.and(next);
    }

    private Boolean statusToActive(String status) {
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus == null || "ALL".equalsIgnoreCase(normalizedStatus)) {
            return null;
        }
        if ("ACTIVE".equalsIgnoreCase(normalizedStatus)) {
            return true;
        }
        if ("INACTIVE".equalsIgnoreCase(normalizedStatus)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid status '" + status + "'. Allowed values: ACTIVE, INACTIVE, ALL");
    }
}
