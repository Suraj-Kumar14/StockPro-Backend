package com.stockpro.product_service.dto.response;

import lombok.Data;

@Data
public class UserProfileSnapshot {

    private Long userId;
    private String name;
    private String email;
    private String role;
}
