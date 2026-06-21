package com.cropdeal.adminservice.query.controller;

import com.cropdeal.adminservice.dto.response.AddOnResponse;
import com.cropdeal.adminservice.dto.response.ApiResponse;
import com.cropdeal.adminservice.query.service.AddOnQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/addons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AddOn Queries", description = "Read add-on listings")
@SecurityRequirement(name = "bearerAuth")
public class AddOnQueryController {

    private final AddOnQueryService addOnQueryService;

    @GetMapping
    @Operation(summary = "Get all add-ons")
    public ResponseEntity<ApiResponse<List<AddOnResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(addOnQueryService.getAll()));
    }

    @GetMapping("/active")
    @Operation(summary = "Get only active add-ons")
    public ResponseEntity<ApiResponse<List<AddOnResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(addOnQueryService.getActive()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get add-on by ID")
    public ResponseEntity<ApiResponse<AddOnResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(addOnQueryService.getById(id)));
    }
}
