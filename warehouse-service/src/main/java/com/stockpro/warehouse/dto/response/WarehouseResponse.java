package com.stockpro.warehouse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Warehouse details response.")
public class WarehouseResponse {

    @Schema(example = "1")
    private Long warehouseId;

    @Schema(example = "Central Distribution Hub")
    private String name;

    @Schema(example = "Bengaluru")
    private String location;

    @Schema(example = "Plot 12, Electronics City Phase 1, Bengaluru")
    private String address;

    @Schema(example = "101")
    private Long managerId;

    @Schema(example = "5000")
    private Integer capacity;

    @Schema(example = "1200")
    private Integer usedCapacity;

    @Schema(example = "true")
    private Boolean isActive;

    @Schema(example = "+91-9876543210")
    private String phone;

    @Schema(example = "2026-04-24T09:30:00")
    private LocalDateTime createdAt;

    @Schema(example = "2026-04-24T09:30:00")
    private LocalDateTime updatedAt;
}
