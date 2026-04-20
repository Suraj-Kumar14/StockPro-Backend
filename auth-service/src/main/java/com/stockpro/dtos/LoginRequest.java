package com.stockpro.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRequest {
	
	@NotNull(message = "email null not allowed")
	private String email;
	@NotNull(message = "password null not allowed")
	private String password;
}
