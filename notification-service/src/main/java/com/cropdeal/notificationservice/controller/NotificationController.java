package com.cropdeal.notificationservice.controller;

import com.cropdeal.notificationservice.dto.ApiResponse;
import com.cropdeal.notificationservice.dto.NotificationResponse;
import com.cropdeal.notificationservice.dto.PagedResponse;
import com.cropdeal.notificationservice.security.AuthenticatedUser;
import com.cropdeal.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification management")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for the authenticated user (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getMyNotifications(principal.getUserId(), pageable)));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get only unread notifications")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getUnread(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadNotifications(principal.getUserId(), pageable)));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread notification count — used for badge in UI")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadCount(principal.getUserId()),
                "Unread count retrieved"));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        notificationService.markAsRead(notificationId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        notificationService.markAllAsRead(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
}
