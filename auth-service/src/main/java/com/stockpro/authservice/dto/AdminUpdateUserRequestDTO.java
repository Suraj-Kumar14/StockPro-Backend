package com.stockpro.authservice.dto;

import com.stockpro.authservice.entity.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUpdateUserRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @Pattern(
            regexp = "^$|^[6-9][0-9]{9}$",
            message = "Phone must be a valid 10-digit Indian mobile number starting with 6-9")
    private String phone;

    @NotNull(message = "Role is required")
    private UserRole role;

    private String department;

    private Boolean isActive;
}
