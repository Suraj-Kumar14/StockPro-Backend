package com.stockpro.alert.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthUserResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;

    @JsonAlias({ "active", "isActive" })
    private Boolean active;
}
