package com.cropdeal.cropservice.controller;

import com.cropdeal.cropservice.dto.ApiResponse;
import com.cropdeal.cropservice.dto.SubscriptionRequest;
import com.cropdeal.cropservice.dto.SubscriptionResponse;
import com.cropdeal.cropservice.security.AuthenticatedUser;
import com.cropdeal.cropservice.service.CropSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Crop Subscriptions", description = "Dealer crop-type subscription management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('DEALER')")
public class CropSubscriptionController {

    private final CropSubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Subscribe to a crop type (Dealer only)")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SubscriptionRequest request) {

        SubscriptionResponse response = subscriptionService.subscribe(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Subscribed successfully"));
    }

    @DeleteMapping("/{subscriptionId}")
    @Operation(summary = "Unsubscribe from a crop type")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        subscriptionService.unsubscribe(subscriptionId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Unsubscribed successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all active subscriptions for the authenticated dealer")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getMySubscriptions(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                subscriptionService.getMySubscriptions(principal.getUserId())));
    }
}
