package com.stockpro.report.dto.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySnapshotCompletedEvent {

    private LocalDate snapshotDate;
    private Long warehouseId;
    private int snapshotCount;
    private LocalDateTime completedAt;
    private String source;
}
