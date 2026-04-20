package com.stockpro.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
	
	@NotNull(message = "email null not allowed")
	private String email;
	@NotNull(message = "password null not allowed")
	private String password;
}
