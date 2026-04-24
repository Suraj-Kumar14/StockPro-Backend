package com.stockpro.warehouse.mapper;

import com.stockpro.warehouse.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouse.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouse.dto.response.WarehouseResponse;
import com.stockpro.warehouse.dto.response.WarehouseSummaryResponse;
import com.stockpro.warehouse.entity.Warehouse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WarehouseMapper {

    public Warehouse toEntity(CreateWarehouseRequest request) {
        return Warehouse.builder()
                .name(normalizeRequired(request.getName()))
                .location(normalizeRequired(request.getLocation()))
                .address(normalizeRequired(request.getAddress()))
                .managerId(request.getManagerId())
                .capacity(request.getCapacity())
                .usedCapacity(0)
                .isActive(Boolean.TRUE)
                .phone(normalizeOptional(request.getPhone()))
                .build();
    }

    public void updateEntity(UpdateWarehouseRequest request, Warehouse warehouse) {
        warehouse.setName(normalizeRequired(request.getName()));
        warehouse.setLocation(normalizeRequired(request.getLocation()));
        warehouse.setAddress(normalizeRequired(request.getAddress()));
        warehouse.setManagerId(request.getManagerId());
        warehouse.setCapacity(request.getCapacity());
        warehouse.setPhone(normalizeOptional(request.getPhone()));
        if (request.getIsActive() != null) {
            warehouse.setIsActive(request.getIsActive());
        }
    }

    public WarehouseResponse toResponse(Warehouse warehouse) {
        return toResponse(warehouse, warehouse.getUsedCapacity());
    }

    public WarehouseResponse toResponse(Warehouse warehouse, Integer usedCapacity) {
        return WarehouseResponse.builder()
                .warehouseId(warehouse.getWarehouseId())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .address(warehouse.getAddress())
                .managerId(warehouse.getManagerId())
                .capacity(warehouse.getCapacity())
                .usedCapacity(usedCapacity)
                .isActive(warehouse.getIsActive())
                .phone(warehouse.getPhone())
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }

    public WarehouseSummaryResponse toSummary(Warehouse warehouse) {
        return WarehouseSummaryResponse.builder()
                .warehouseId(warehouse.getWarehouseId())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .isActive(warehouse.getIsActive())
                .build();
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
