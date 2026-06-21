package com.cropdeal.userservice.dto;

import com.cropdeal.userservice.entity.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String name;
    private String email;
    private UserRole role;
}
