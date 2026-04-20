package com.stockpro.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
	@NotNull(message = "Name null not allowed")
	private String fullName;
	@NotNull(message = "email null not allowed")
    private String email;
	@NotNull(message = "phone null not allowed")
    private String phone;
}