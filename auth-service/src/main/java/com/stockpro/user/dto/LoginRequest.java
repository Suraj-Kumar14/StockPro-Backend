package com.stockpro.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {
	
	@NotNull(message = "email null not allowed")
	private String email;
	@NotNull(message = "password null not allowed")
	private String password;

}