package com.stockpro.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration Request DTO
 *
 * role is OPTIONAL - if not provided, defaults to WAREHOUSE_STAFF
 * Valid roles: ADMIN, INVENTORY_MANAGER, WAREHOUSE_STAFF, PURCHASE_OFFICER
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
	@NotNull(message = "Name null not allowed")
	private String fullName;
	@NotNull(message = "email null not allowed")
    private String email;
	@NotNull(message = "password null not allowed")
    private String password;
	@NotNull(message = "phone null not allowed")
    private String phone;
	private String department;
	private String role; // Optional - defaults to WAREHOUSE_STAFF if not provided
}
