package com.cropdeal.adminservice.client;

import com.cropdeal.adminservice.dto.response.ApiResponse;
import com.cropdeal.adminservice.dto.response.PagedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users")
    ApiResponse<PagedResponse<Map<String, Object>>> getAllUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String role);

    @PatchMapping("/users/{userId}/status")
    ApiResponse<Map<String, Object>> toggleUserStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId);
}
