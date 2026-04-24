package com.stockpro.warehouse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Compact warehouse summary response.")
public class WarehouseSummaryResponse {

    @Schema(example = "1")
    private Long warehouseId;

    @Schema(example = "Central Distribution Hub")
    private String name;

    @Schema(example = "Bengaluru")
    private String location;

    @Schema(example = "true")
    private Boolean isActive;
}
