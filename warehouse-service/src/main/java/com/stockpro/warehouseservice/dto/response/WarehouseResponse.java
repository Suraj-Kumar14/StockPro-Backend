package com.stockpro.warehouseservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record WarehouseResponse(
        Long warehouseId,
        String name,
        String code,
        String location,
        String address,
        String city,
        String state,
        String country,
        Long managerId,
        Integer capacity,
        Integer usedCapacity,
        Integer availableCapacity,
        Double utilizationPercentage,
        Boolean isActive,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    @JsonProperty("id")
    public Long id() {
        return warehouseId;
    }

    @JsonProperty("warehouseName")
    public String warehouseName() {
        return name;
    }

    @JsonProperty("warehouseCode")
    public String warehouseCode() {
        return code;
    }

    @JsonProperty("active")
    public Boolean active() {
        return isActive;
    }

    @JsonProperty("utilization")
    public Double utilization() {
        return utilizationPercentage;
    }
}
