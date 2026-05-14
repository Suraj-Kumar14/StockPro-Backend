package com.stockpro.authservice.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockpro.authservice.entity.UserRole;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private String department;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    @JsonProperty("id")
    public Long getId() {
        return userId;
    }

    @JsonProperty("fullName")
    public String getFullName() {
        return name;
    }

    @JsonProperty("active")
    public Boolean getActive() {
        return isActive;
    }

    @JsonProperty("status")
    public String getStatus() {
        return Boolean.TRUE.equals(isActive) ? "ACTIVE" : "INACTIVE";
    }
}
