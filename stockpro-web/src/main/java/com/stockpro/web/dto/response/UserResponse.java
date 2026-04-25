package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;

    @JsonAlias({ "active", "isActive" })
    private boolean active;

    private String department;
}
