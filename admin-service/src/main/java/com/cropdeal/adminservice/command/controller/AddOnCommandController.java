package com.cropdeal.adminservice.command.controller;

import com.cropdeal.adminservice.command.service.AddOnCommandService;
import com.cropdeal.adminservice.dto.request.AddOnRequest;
import com.cropdeal.adminservice.dto.response.AddOnResponse;
import com.cropdeal.adminservice.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/addons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AddOn Commands", description = "Create, update, toggle, delete add-ons")
@SecurityRequirement(name = "bearerAuth")
public class AddOnCommandController {

    private final AddOnCommandService addOnCommandService;

    @PostMapping
    @Operation(summary = "Create a new add-on")
    public ResponseEntity<ApiResponse<AddOnResponse>> create(@Valid @RequestBody AddOnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(addOnCommandService.create(request), "AddOn created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing add-on")
    public ResponseEntity<ApiResponse<AddOnResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AddOnRequest request) {
        return ResponseEntity.ok(ApiResponse.success(addOnCommandService.update(id, request), "AddOn updated"));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle add-on active/inactive status")
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable Long id) {
        addOnCommandService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success(null, "AddOn status toggled"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an add-on permanently")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        addOnCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "AddOn deleted"));
    }
}
