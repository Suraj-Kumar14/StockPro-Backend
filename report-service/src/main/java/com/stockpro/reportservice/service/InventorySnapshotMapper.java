package com.stockpro.reportservice.service;

import com.stockpro.reportservice.dto.InventorySnapshotDTO;
import com.stockpro.reportservice.entity.InventorySnapshot;
import org.springframework.stereotype.Component;

@Component
public class InventorySnapshotMapper {

    public InventorySnapshotDTO toDto(InventorySnapshot snapshot) {
        return InventorySnapshotDTO.builder()
                .snapshotId(snapshot.getSnapshotId())
                .warehouseId(snapshot.getWarehouseId())
                .productId(snapshot.getProductId())
                .quantity(snapshot.getQuantity())
                .stockValue(snapshot.getStockValue())
                .snapshotDate(snapshot.getSnapshotDate())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }
}
