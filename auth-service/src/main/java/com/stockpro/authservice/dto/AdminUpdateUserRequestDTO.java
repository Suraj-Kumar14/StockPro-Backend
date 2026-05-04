package com.stockpro.authservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUpdateUserRequestDTO {
    @NotBlank(message = "Full name is required")
    @JsonAlias({"fullName", "name"})
    private String name;

    @Pattern(
            regexp = "^$|^[6-9][0-9]{9}$",
            message = "Phone must be a valid 10-digit Indian mobile number starting with 6-9")
    private String phone;

    private String department;

    private Boolean isActive;
}
