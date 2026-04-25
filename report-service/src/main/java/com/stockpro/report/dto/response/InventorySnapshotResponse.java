package com.stockpro.report.dto.response;

import java.math.BigDecimal;
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
public class InventorySnapshotResponse {

    private Long snapshotId;
    private Long warehouseId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal stockValue;
    private LocalDate snapshotDate;
    private LocalDateTime createdAt;
}
