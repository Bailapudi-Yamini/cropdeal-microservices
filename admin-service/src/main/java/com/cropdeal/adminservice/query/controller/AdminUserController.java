package com.cropdeal.adminservice.query.controller;

import com.cropdeal.adminservice.client.UserServiceClient;
import com.cropdeal.adminservice.dto.response.ApiResponse;
import com.cropdeal.adminservice.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "User management proxied from user-service")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserServiceClient userServiceClient;

    @GetMapping("/users")
    @Operation(summary = "List all users with optional role filter")
    public ResponseEntity<ApiResponse<PagedResponse<Map<String, Object>>>> getUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String role) {

        try {
            return ResponseEntity.ok(
                    userServiceClient.getAllUsers(authHeader, page, size, role));
        } catch (Exception e) {
            log.warn("Could not fetch users from user-service: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(
                    PagedResponse.<Map<String, Object>>builder()
                            .content(List.of())
                            .page(page).size(size)
                            .totalElements(0).totalPages(0).last(true)
                            .build(),
                    "No users found"));
        }
    }

    @PatchMapping("/users/{userId}/toggle")
    @Operation(summary = "Toggle user active/inactive status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleUser(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            return ResponseEntity.ok(
                    userServiceClient.toggleUserStatus(authHeader, userId));
        } catch (Exception e) {
            log.warn("Could not toggle user {} in user-service: {}", userId, e.getMessage());
            return ResponseEntity.ok(
                    ApiResponse.error("Failed to toggle user: " + e.getMessage()));
        }
    }
}
