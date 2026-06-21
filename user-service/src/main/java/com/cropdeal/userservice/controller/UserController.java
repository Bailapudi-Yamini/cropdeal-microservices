package com.cropdeal.userservice.controller;

import com.cropdeal.userservice.dto.*;
import com.cropdeal.userservice.entity.UserRole;
import com.cropdeal.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Profile and bank account operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users paged (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsersPaged(page, size, role)));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Toggle user active status (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.toggleUserStatus(userId)));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(userId)));
    }

    @PutMapping("/profile/{userId}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request), "Profile updated"));
    }

    @DeleteMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate a user account")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated"));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get all users by role (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsersByRole(role)));
    }

    @PostMapping("/{userId}/bank-accounts")
    @Operation(summary = "Add a bank account for a user")
    public ResponseEntity<ApiResponse<BankAccountResponse>> addBankAccount(
            @PathVariable Long userId,
            @Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.addBankAccount(userId, request), "Bank account added"));
    }

    @GetMapping("/{userId}/bank-accounts")
    @Operation(summary = "Get all bank accounts for a user")
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getBankAccounts(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getBankAccounts(userId)));
    }
}
