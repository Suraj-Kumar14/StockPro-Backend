package com.stockpro.authservice.dto;

import com.stockpro.authservice.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminChangeUserRoleRequestDTO {
    @NotNull(message = "Role is required")
    private UserRole role;
}
