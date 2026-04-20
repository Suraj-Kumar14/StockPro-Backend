package com.stockpro.Mapper;

import com.stockpro.dtos.UserResponseDTO;
import com.stockpro.entity.User;

public class UserMapper {
    public static UserResponseDTO toResponse(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .department(user.getDepartment())
                .isActive(user.isActive())
                .build();
    }
}