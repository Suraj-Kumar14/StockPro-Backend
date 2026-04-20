package com.stockpro.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean isActive;
    private String department;
}