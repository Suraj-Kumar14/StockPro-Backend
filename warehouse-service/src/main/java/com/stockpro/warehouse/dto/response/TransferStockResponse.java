package com.stockpro.warehouse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
@Schema(description = "Response returned after a successful stock transfer.")
public class TransferStockResponse {

    @Schema(example = "501")
    private Long productId;

    @Schema(example = "1")
    private Long sourceWarehouseId;

    @Schema(example = "2")
    private Long destinationWarehouseId;

    @Schema(example = "12.0000")
    private BigDecimal transferredQuantity;

    @Schema(example = "88.0000")
    private BigDecimal sourceBalance;

    @Schema(example = "42.0000")
    private BigDecimal destinationBalance;

    @Schema(example = "Stock transferred successfully.")
    private String message;
}
