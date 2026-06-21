package com.cropdeal.userservice.dto;

import com.cropdeal.userservice.entity.OAuthProvider;
import com.cropdeal.userservice.entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private String profileImageUrl;
    private boolean active;
    private OAuthProvider oauthProvider;
    private LocalDateTime createdAt;
}
