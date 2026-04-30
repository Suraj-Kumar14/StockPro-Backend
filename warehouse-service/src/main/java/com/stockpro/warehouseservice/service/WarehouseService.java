package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.WarehouseRequestDTO;
import com.stockpro.warehouseservice.dto.WarehouseResponseDTO;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.CapacityExceededException;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;

    @Transactional
    public WarehouseResponseDTO createWarehouse(WarehouseRequestDTO dto) {
        log.info("Creating warehouse: {}", dto.getName());

        if (warehouseRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException(
                    "Warehouse with name '" + dto.getName() + "' already exists");
        }

        Warehouse warehouse = Warehouse.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .address(dto.getAddress())
                .managerId(dto.getManagerId())
                .capacity(dto.getCapacity())
                .usedCapacity(0)
                .phone(dto.getPhone())
                .isActive(true)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created with ID: {}", saved.getWarehouseId());
        return mapToDTO(saved);
    }

    public WarehouseResponseDTO getWarehouseById(Long id) {
        log.info("Fetching warehouse with ID: {}", id);
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + id));
        return mapToDTO(warehouse);
    }

    public List<WarehouseResponseDTO> getAllWarehouses() {
        log.info("Fetching all warehouses");
        return warehouseRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<WarehouseResponseDTO> getActiveWarehouses() {
        log.info("Fetching active warehouses");
        return warehouseRepository.findByIsActive(true)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<WarehouseResponseDTO> getWarehousesByManager(Long managerId) {
        log.info("Fetching warehouses for manager: {}", managerId);
        return warehouseRepository.findByManagerId(managerId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public WarehouseResponseDTO updateWarehouse(Long id, WarehouseRequestDTO dto) {
        log.info("Updating warehouse with ID: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + id));

        if (!warehouse.getName().equals(dto.getName())
                && warehouseRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException(
                    "Warehouse with name '" + dto.getName() + "' already exists");
        }

        int actualUsedCapacity = defaultIfNull(
                stockLevelRepository.sumQuantityByWarehouseId(id));
        if (dto.getCapacity() < actualUsedCapacity) {
            throw new CapacityExceededException(
                    "Warehouse capacity cannot be reduced below current stock usage: "
                            + actualUsedCapacity);
        }

        warehouse.setName(dto.getName());
        warehouse.setLocation(dto.getLocation());
        warehouse.setAddress(dto.getAddress());
        warehouse.setManagerId(dto.getManagerId());
        warehouse.setCapacity(dto.getCapacity());
        warehouse.setUsedCapacity(actualUsedCapacity);
        warehouse.setPhone(dto.getPhone());

        Warehouse updated = warehouseRepository.save(warehouse);
        log.info("Warehouse updated with ID: {}", id);
        return mapToDTO(updated);
    }

    @Transactional
    public void deactivateWarehouse(Long id) {
        log.info("Deactivating warehouse with ID: {}", id);
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + id));
        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
        log.info("Warehouse deactivated: {}", id);
    }

    @Transactional
    public void assignManager(Long warehouseId, Long managerId) {
        log.info("Assigning manager {} to warehouse {}", managerId, warehouseId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + warehouseId));
        warehouse.setManagerId(managerId);
        warehouseRepository.save(warehouse);
    }

    private WarehouseResponseDTO mapToDTO(Warehouse warehouse) {
        return WarehouseResponseDTO.builder()
                .warehouseId(warehouse.getWarehouseId())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .address(warehouse.getAddress())
                .managerId(warehouse.getManagerId())
                .capacity(warehouse.getCapacity())
                .usedCapacity(warehouse.getUsedCapacity())
                .phone(warehouse.getPhone())
                .isActive(warehouse.getIsActive())
                .createdAt(warehouse.getCreatedAt())
                .build();
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
