package com.cropdeal.userservice.dto;

import com.cropdeal.userservice.entity.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String phone;

    private String location;

    @NotNull(message = "Role is required")
    private UserRole role;
}
