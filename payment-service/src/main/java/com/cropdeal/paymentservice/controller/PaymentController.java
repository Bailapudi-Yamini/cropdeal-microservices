package com.cropdeal.paymentservice.controller;

import com.cropdeal.paymentservice.dto.request.InitiatePaymentRequest;
import com.cropdeal.paymentservice.dto.request.RefundRequest;
import com.cropdeal.paymentservice.dto.request.VerifyPaymentRequest;
import com.cropdeal.paymentservice.dto.response.*;
import com.cropdeal.paymentservice.entity.PaymentStatus;
import com.cropdeal.paymentservice.security.AuthenticatedUser;
import com.cropdeal.paymentservice.service.PaymentService;
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
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing, Event Sourcing audit trail, and receipt management")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    // ── Payment lifecycle ─────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Manually initiate a payment for a confirmed order (Dealer only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment initiated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate payment for this order")
    })
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody InitiatePaymentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        paymentService.initiatePayment(principal.getUserId(), request),
                        "Payment initiated"));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details by ID (farmer or dealer participant)")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentById(paymentId, principal.getUserId())));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment status by order ID — poll this after order.confirmed to get razorpayOrderId")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentByOrderId(orderId, principal.getUserId())));
    }

    @GetMapping("/dealer")
    @Operation(summary = "Get all payments for the authenticated dealer")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getDealerPayments(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentsForDealer(principal.getUserId(), status, pageable)));
    }

    @GetMapping("/farmer")
    @Operation(summary = "Get all payments for the authenticated farmer")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getFarmerPayments(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentsForFarmer(principal.getUserId(), status, pageable)));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a successful payment (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.processRefund(paymentId, request),
                "Payment refunded successfully"));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay signature and mark payment SUCCESS. Send orderId OR paymentId + razorpay fields.")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody VerifyPaymentRequest request) {

        if (request.getPaymentId() == null && request.getOrderId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Either paymentId or orderId must be provided"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.verifyPayment(request, principal.getUserId()),
                "Payment verified successfully"));
    }

    // ── Event Sourcing audit trail ────────────────────────────────────────────

    @GetMapping("/{paymentId}/events")
    @Operation(summary = "Get full Event Sourcing audit trail for a payment (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentEventResponse>>> getEventStream(
            @PathVariable Long paymentId) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getEventStream(paymentId),
                "Event stream retrieved"));
    }

    // ── Receipts ──────────────────────────────────────────────────────────────

    @GetMapping("/{paymentId}/receipt")
    @Operation(summary = "Get receipt for a successful payment (farmer or dealer)")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceipt(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getReceipt(paymentId, principal.getUserId())));
    }

    @GetMapping("/receipts/my")
    @Operation(summary = "Get all receipts for the authenticated user (farmer or dealer)")
    public ResponseEntity<ApiResponse<PagedResponse<ReceiptResponse>>> getMyReceipts(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt"));
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getMyReceipts(principal.getUserId(), principal.getRole(), pageable)));
    }
}
