package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WarehouseResponse {

    private Long warehouseId;
    private String name;
    private String location;
    private String address;
    private Long managerId;
    private Integer capacity;
    private Integer usedCapacity;

    @JsonAlias({ "active", "isActive" })
    private Boolean active;

    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
