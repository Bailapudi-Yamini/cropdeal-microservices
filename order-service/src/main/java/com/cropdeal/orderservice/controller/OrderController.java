package com.cropdeal.orderservice.controller;

import com.cropdeal.orderservice.dto.request.NegotiationRequest;
import com.cropdeal.orderservice.dto.request.PlaceOrderRequest;
import com.cropdeal.orderservice.dto.response.ApiResponse;
import com.cropdeal.orderservice.dto.response.NegotiationResponse;
import com.cropdeal.orderservice.dto.response.OrderResponse;
import com.cropdeal.orderservice.dto.response.PagedResponse;
import com.cropdeal.orderservice.entity.OrderStatus;
import com.cropdeal.orderservice.security.AuthenticatedUser;
import com.cropdeal.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, lifecycle, and negotiation management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    // ── Order lifecycle ───────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Place a new order on a crop listing (Dealer only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not a dealer")
    })
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PlaceOrderRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        orderService.placeOrder(principal.getUserId(), request),
                        "Order placed successfully"));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details (farmer or dealer participant only)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderById(orderId, principal.getUserId())));
    }

    @GetMapping("/dealer")
    @Operation(summary = "Get all orders for the authenticated dealer")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getDealerOrders(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrdersForDealer(principal.getUserId(), status, pageable)));
    }

    @GetMapping("/farmer")
    @Operation(summary = "Get all orders for the authenticated farmer")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getFarmerOrders(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrdersForFarmer(principal.getUserId(), status, pageable)));
    }

    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "Farmer confirms a PENDING order directly (no negotiation)")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.confirmOrder(orderId, principal.getUserId()),
                "Order confirmed"));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel an order (Dealer only, not yet COMPLETED)")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        orderService.cancelOrder(orderId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Order cancelled"));
    }

    // ── Negotiation flow ──────────────────────────────────────────────────────

    @PutMapping("/{orderId}/negotiate")
    @Operation(summary = "Propose or counter a price (farmer or dealer)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Negotiation proposed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Max rounds reached or invalid state")
    })
    public ResponseEntity<ApiResponse<NegotiationResponse>> proposeNegotiation(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody NegotiationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        orderService.proposeNegotiation(orderId, principal.getUserId(), request),
                        "Negotiation proposal submitted"));
    }

    @PutMapping("/{orderId}/accept")
    @Operation(summary = "Accept the latest pending negotiation proposal (farmer or dealer)")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptNegotiation(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.acceptNegotiation(orderId, principal.getUserId()),
                "Negotiation accepted — order confirmed"));
    }

    @PutMapping("/{orderId}/reject")
    @Operation(summary = "Reject the latest pending negotiation proposal (farmer or dealer)")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectNegotiation(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.rejectNegotiation(orderId, principal.getUserId()),
                "Negotiation rejected — order cancelled"));
    }

    @GetMapping("/{orderId}/negotiations")
    @Operation(summary = "Get full negotiation history for an order")
    public ResponseEntity<ApiResponse<List<NegotiationResponse>>> getNegotiationHistory(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.getNegotiationHistory(orderId, principal.getUserId())));
    }
}
