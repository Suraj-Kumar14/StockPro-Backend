package com.stockpro.report.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
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
@Schema(description = "Request used to manually trigger an inventory snapshot.")
public class TakeSnapshotRequest {

    @NotNull(message = "snapshotDate is required.")
    @Schema(example = "2026-04-25")
    private LocalDate snapshotDate;

    @Positive(message = "warehouseId must be greater than zero.")
    private Long warehouseId;
}
