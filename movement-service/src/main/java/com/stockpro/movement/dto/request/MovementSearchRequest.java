package com.stockpro.movement.dto.request;

import com.stockpro.movement.enums.MovementType;
import com.stockpro.movement.validation.ValidDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@ValidDateRange
@Schema(description = "Advanced movement search request with optional filters and pagination.")
public class MovementSearchRequest {

    @Positive(message = "productId must be greater than zero.")
    private Long productId;

    @Positive(message = "warehouseId must be greater than zero.")
    private Long warehouseId;

    private MovementType movementType;

    @Positive(message = "referenceId must be greater than zero.")
    private Long referenceId;

    @Positive(message = "performedBy must be greater than zero.")
    private Long performedBy;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Min(value = 0, message = "page cannot be negative.")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "size must be greater than zero.")
    @Builder.Default
    private Integer size = 20;

    @Size(max = 50, message = "sortBy cannot be longer than 50 characters.")
    @Builder.Default
    private String sortBy = "movementDate";

    @Size(max = 4, message = "sortDir must be asc or desc.")
    @Builder.Default
    private String sortDir = "desc";
}
