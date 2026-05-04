package com.stockpro.authservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.stockpro.authservice.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminCreateUserRequestDTO {
    @NotBlank(message = "Full name is required")
    @JsonAlias({"fullName", "name"})
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(
            regexp = "^$|^[6-9][0-9]{9}$",
            message = "Phone must be a valid 10-digit Indian mobile number starting with 6-9")
    private String phone;

    @NotNull(message = "Role is required")
    private UserRole role;

    private String department;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
            message = "Password must contain uppercase, lowercase, number, and special character")
    private String password;

    private Boolean isActive = Boolean.TRUE;
}
