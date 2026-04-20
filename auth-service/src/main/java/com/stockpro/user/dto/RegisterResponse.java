package com.stockpro.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegisterResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String department;
    private boolean emailVerified;
    private boolean active;
    private String message;
}