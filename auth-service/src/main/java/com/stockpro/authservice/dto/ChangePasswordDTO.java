package com.stockpro.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank
    private String oldPassword;

    private String currentPassword;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
            message = "Password must contain uppercase, lowercase, number, and special character")
    private String newPassword;

    private String confirmPassword;

    public String getEffectiveCurrentPassword() {
        return oldPassword != null ? oldPassword : currentPassword;
    }
}
