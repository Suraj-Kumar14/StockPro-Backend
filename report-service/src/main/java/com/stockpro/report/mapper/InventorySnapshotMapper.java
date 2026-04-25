package com.stockpro.report.mapper;

import com.stockpro.report.dto.response.InventorySnapshotResponse;
import com.stockpro.report.entity.InventorySnapshot;
import org.springframework.stereotype.Component;

@Component
public class InventorySnapshotMapper {

    public InventorySnapshotResponse toResponse(InventorySnapshot snapshot) {
        return InventorySnapshotResponse.builder()
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
