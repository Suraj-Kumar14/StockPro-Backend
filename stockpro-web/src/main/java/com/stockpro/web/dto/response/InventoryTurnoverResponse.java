package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryTurnoverResponse {

    private Long productId;
    private String productName;
    private BigDecimal turnoverRate;
    private LocalDate fromDate;
    private LocalDate toDate;
}
