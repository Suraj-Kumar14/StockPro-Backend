package com.stockpro.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Entity
 *
 * ROLE VALUES (see config.Roles):
 * - ADMIN: Full system access
 * - INVENTORY_MANAGER: Manage inventory module
 * - WAREHOUSE_STAFF: Manage warehouse operations (default role)
 * - PURCHASE_OFFICER: Manage purchase module
 */
@Entity
@Data
@Table(name="users")
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;    
    private String fullName;
    @Column(unique = true, nullable = false)
    private String email;
    private String pendingEmail; // Used when user changes email - stores new email until OTP verified
    private String passwordHash; // Hashed password using BCrypt
    private String phone;
    private String role; // ADMIN, INVENTORY_MANAGER, WAREHOUSE_STAFF, PURCHASE_OFFICER
    private String department; // Department assignment for access control
    private boolean isActive; // Whether account is verified (OTP confirmed)
    private String otpCode; // 6-digit OTP for email verification
    private LocalDateTime otpExpiry; // OTP expiry time (5 minutes)
    private LocalDateTime lastLoginAt; // Track last login time
    private LocalDateTime createdAt = LocalDateTime.now(); // Account creation time
}

