package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.dto.request.InitiatePaymentRequest;
import com.cropdeal.paymentservice.dto.request.RefundRequest;
import com.cropdeal.paymentservice.dto.request.VerifyPaymentRequest;
import com.cropdeal.paymentservice.dto.response.PagedResponse;
import com.cropdeal.paymentservice.dto.response.PaymentEventResponse;
import com.cropdeal.paymentservice.dto.response.PaymentResponse;
import com.cropdeal.paymentservice.dto.response.ReceiptResponse;
import com.cropdeal.paymentservice.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {

    // ── Payment lifecycle ─────────────────────────────────────────────────────
    PaymentResponse initiatePayment(Long dealerId, InitiatePaymentRequest request);

    PaymentResponse getPaymentById(Long paymentId, Long callerId);

    PaymentResponse getPaymentByOrderId(Long orderId, Long callerId);

    PagedResponse<PaymentResponse> getPaymentsForDealer(Long dealerId, PaymentStatus status, Pageable pageable);

    PagedResponse<PaymentResponse> getPaymentsForFarmer(Long farmerId, PaymentStatus status, Pageable pageable);

    PaymentResponse processRefund(Long paymentId, RefundRequest request);

    PaymentResponse verifyPayment(VerifyPaymentRequest request, Long callerId);

    // ── Event Sourcing ────────────────────────────────────────────────────────
    List<PaymentEventResponse> getEventStream(Long paymentId);

    // ── Receipt ───────────────────────────────────────────────────────────────
    ReceiptResponse getReceipt(Long paymentId, Long callerId);

    PagedResponse<ReceiptResponse> getMyReceipts(Long userId, String role, Pageable pageable);

    // ── Internal (called by EventConsumer) ───────────────────────────────────
    void handleOrderConfirmed(Long orderId, Long farmerId, Long dealerId,
                               Long cropListingId, Double amount);
}
