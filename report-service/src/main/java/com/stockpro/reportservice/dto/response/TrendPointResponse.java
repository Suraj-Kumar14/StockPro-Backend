package com.stockpro.reportservice.dto.response;

import com.stockpro.reportservice.enums.TrendDirection;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPointResponse(LocalDate date, BigDecimal value, TrendDirection direction) {
}
