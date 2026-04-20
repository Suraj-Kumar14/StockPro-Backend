package com.stockpro.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String email;
    private String pendingEmail;
    private String passwordHash;
    private String phone;
    private String role;
    private String department;
    private boolean isActive;
    private String otpCode;
    private LocalDateTime otpExpiry;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}