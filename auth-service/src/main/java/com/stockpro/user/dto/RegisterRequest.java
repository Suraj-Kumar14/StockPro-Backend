package com.stockpro.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
	
	@NotNull(message = "Name null not allowed")
    private String fullName;
	@NotNull(message = "email null not allowed")
    private String email;
	@NotNull(message = "password null not allowed")
    private String passwordHash;
	@NotNull(message = "phone null not allowed")
    private String phone;
	@NotNull(message = "must enter valid role")
    private String role;
	@NotNull(message = "must enter valid department")
    private String department;
}