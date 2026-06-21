package com.cropdeal.cropservice.controller;

import com.cropdeal.cropservice.dto.*;
import com.cropdeal.cropservice.entity.CropType;
import com.cropdeal.cropservice.security.AuthenticatedUser;
import com.cropdeal.cropservice.service.CropListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crops")
@RequiredArgsConstructor
@Tag(name = "Crop Listings", description = "Farmer crop listing management")
@SecurityRequirement(name = "bearerAuth")
public class CropListingController {

    private final CropListingService cropListingService;

    @PostMapping
    @Operation(summary = "Create a new crop listing (Farmer only)")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<CropListingResponse>> createListing(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CropListingRequest request) {

        CropListingResponse response = cropListingService.createListing(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Crop listing created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a crop listing by ID (public)")
    public ResponseEntity<ApiResponse<CropListingResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(cropListingService.getListingById(id)));
    }

    @GetMapping
    @Operation(summary = "Browse available crop listings with optional filters (public)")
    public ResponseEntity<ApiResponse<PagedResponse<CropListingResponse>>> getAvailable(
            @RequestParam(required = false) CropType cropType,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return ResponseEntity.ok(ApiResponse.success(
                cropListingService.getAvailableListings(cropType, location, pageable)));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search on crop name and location (MongoDB-backed)")
    public ResponseEntity<ApiResponse<PagedResponse<CropListingResponse>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                cropListingService.searchListings(keyword, pageable)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all listings for the authenticated farmer")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<List<CropListingResponse>>> getMyListings(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                cropListingService.getMyListings(principal.getUserId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a crop listing (owner farmer only)")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<CropListingResponse>> updateListing(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateCropRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                cropListingService.updateListing(id, principal.getUserId(), request),
                "Crop listing updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a crop listing (owner farmer only)")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Void>> deleteListing(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        cropListingService.deleteListing(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Crop listing deactivated"));
    }
}
