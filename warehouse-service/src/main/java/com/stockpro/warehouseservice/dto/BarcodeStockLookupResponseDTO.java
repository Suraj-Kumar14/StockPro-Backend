package com.stockpro.warehouseservice.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarcodeStockLookupResponseDTO {

    private ProductLookupResponseDTO product;
    private List<StockLevelResponseDTO> stockLevels;
}
