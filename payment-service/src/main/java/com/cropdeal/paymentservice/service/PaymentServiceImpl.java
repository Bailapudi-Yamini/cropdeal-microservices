package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.dto.request.InitiatePaymentRequest;
import com.cropdeal.paymentservice.dto.request.RefundRequest;
import com.cropdeal.paymentservice.dto.request.VerifyPaymentRequest;
import com.cropdeal.paymentservice.dto.response.PagedResponse;
import com.cropdeal.paymentservice.dto.response.PaymentEventResponse;
import com.cropdeal.paymentservice.dto.response.PaymentResponse;
import com.cropdeal.paymentservice.dto.response.ReceiptResponse;
import com.cropdeal.paymentservice.entity.*;
import com.cropdeal.paymentservice.event.PaymentCheckoutReadyEvent;
import com.cropdeal.paymentservice.event.PaymentSuccessEvent;
import com.cropdeal.paymentservice.exception.DuplicatePaymentException;
import com.cropdeal.paymentservice.exception.InvalidPaymentStateException;
import com.cropdeal.paymentservice.exception.ResourceNotFoundException;
import com.cropdeal.paymentservice.exception.UnauthorizedActionException;
import com.cropdeal.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventSourcingService eventSourcingService;
    private final ReceiptService receiptService;
    private final EventPublisher eventPublisher;
    private final PaymentMapper paymentMapper;
    private final RazorpayGateway razorpayGateway;

    // ── Payment lifecycle ─────────────────────────────────────────────────────

    @Override
    public PaymentResponse initiatePayment(Long dealerId, InitiatePaymentRequest request) {
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            throw new DuplicatePaymentException(
                    "Payment already exists for orderId: " + request.getOrderId());
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .dealerId(dealerId)
                .farmerId(0L)
                .amount(request.getAmount())
                .status(PaymentStatus.INITIATED)
                .transactionId(transactionId)
                .build();

        payment = paymentRepository.save(payment);

        String razorpayOrderId = razorpayGateway.createOrder(
                payment.getId(), request.getOrderId(), request.getAmount());
        payment.setRazorpayOrderId(razorpayOrderId);
        payment = paymentRepository.save(payment);

        eventSourcingService.appendEvent(payment.getId(), PaymentEventType.PAYMENT_INITIATED,
                buildEventPayload(payment, "Payment initiated by dealer"));

        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, Long callerId) {
        Payment payment = findById(paymentId);
        assertParticipant(payment, callerId);
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, Long callerId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for orderId: " + orderId));
        assertParticipant(payment, callerId);
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentsForDealer(Long dealerId, PaymentStatus status,
                                                                Pageable pageable) {
        Page<Payment> page = (status != null)
                ? paymentRepository.findByDealerIdAndStatus(dealerId, status, pageable)
                : paymentRepository.findByDealerId(dealerId, pageable);
        return toPagedResponse(page.map(paymentMapper::toPaymentResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentsForFarmer(Long farmerId, PaymentStatus status,
                                                                Pageable pageable) {
        Page<Payment> page = (status != null)
                ? paymentRepository.findByFarmerIdAndStatus(farmerId, status, pageable)
                : paymentRepository.findByFarmerId(farmerId, pageable);
        return toPagedResponse(page.map(paymentMapper::toPaymentResponse));
    }

    @Override
    public PaymentResponse verifyPayment(VerifyPaymentRequest request, Long callerId) {
        Payment payment = (request.getPaymentId() != null)
                ? findById(request.getPaymentId())
                : paymentRepository.findByOrderId(request.getOrderId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Payment not found for orderId: " + request.getOrderId()));
        assertParticipant(payment, callerId);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return paymentMapper.toPaymentResponse(payment);
        }

        boolean valid = razorpayGateway.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("{\"error\":\"Invalid signature\"}");
            paymentRepository.save(payment);
            eventSourcingService.appendEvent(payment.getId(), PaymentEventType.PAYMENT_FAILED,
                    buildEventPayload(payment, "Invalid Razorpay signature"));
            try {
                eventPublisher.publishPaymentFailed(com.cropdeal.paymentservice.event.PaymentFailedEvent.builder()
                        .paymentId(payment.getId())
                        .orderId(payment.getOrderId())
                        .farmerId(payment.getFarmerId())
                        .dealerId(payment.getDealerId())
                        .amount(payment.getAmount())
                        .transactionId(request.getRazorpayPaymentId())
                        .failureReason("Invalid Razorpay signature")
                        .failedAt(LocalDateTime.now())
                        .build());
            } catch (Exception ex) {
                log.warn("Failed to publish payment.failed for paymentId={}: {}", payment.getId(), ex.getMessage());
            }
            throw new InvalidPaymentStateException("Payment verification failed: invalid signature");
        }

        // Mark success directly via save instead of @Modifying query
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentGatewayRef(request.getRazorpayPaymentId());
        payment.setGatewayResponse("{\"razorpay_payment_id\":\"" + request.getRazorpayPaymentId() + "\"}");
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        eventSourcingService.appendEvent(payment.getId(), PaymentEventType.PAYMENT_SUCCESS,
                buildEventPayload(payment, "Razorpay verified: " + request.getRazorpayPaymentId()));

        String cropDetails = buildCropDetails(payment.getOrderId(), payment.getAmount());
        Receipt receipt = receiptService.generateReceipt(payment, cropDetails);

        try {
            eventPublisher.publishPaymentSuccess(PaymentSuccessEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .cropListingId(null)
                    .farmerId(payment.getFarmerId())
                    .dealerId(payment.getDealerId())
                    .amount(payment.getAmount())
                    .transactionId(request.getRazorpayPaymentId())
                    .receiptNumber(receipt.getReceiptNumber())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish payment.success for paymentId={}: {}", payment.getId(), e.getMessage());
        }

        log.info("Payment {} verified and marked SUCCESS via Razorpay", payment.getId());
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    public PaymentResponse processRefund(Long paymentId, RefundRequest request) {
        Payment payment = findById(paymentId);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStateException(
                    "Only successful payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setGatewayResponse("REFUNDED: " + request.getReason());
        payment = paymentRepository.save(payment);

        eventSourcingService.appendEvent(payment.getId(), PaymentEventType.PAYMENT_REFUNDED,
                buildEventPayload(payment, "Refund reason: " + request.getReason()));

        log.info("Payment {} refunded. Reason: {}", paymentId, request.getReason());
        return paymentMapper.toPaymentResponse(payment);
    }

    // ── Event Sourcing ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PaymentEventResponse> getEventStream(Long paymentId) {
        findById(paymentId);
        return eventSourcingService.getEventStream(paymentId);
    }

    // ── Receipt ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(Long paymentId, Long callerId) {
        Payment payment = findById(paymentId);
        assertParticipant(payment, callerId);
        return receiptService.getReceiptByPaymentId(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReceiptResponse> getMyReceipts(Long userId, String role, Pageable pageable) {
        return "FARMER".equals(role)
                ? receiptService.getReceiptsForFarmer(userId, pageable)
                : receiptService.getReceiptsForDealer(userId, pageable);
    }

    // ── Internal (called by EventConsumer) ────────────────────────────────────

    @Override
    public void handleOrderConfirmed(Long orderId, Long farmerId, Long dealerId,
                                      Long cropListingId, Double amount) {
        if (paymentRepository.existsByOrderId(orderId)) {
            log.warn("Payment already exists for orderId={}, skipping", orderId);
            return;
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Payment payment = Payment.builder()
                .orderId(orderId)
                .farmerId(farmerId)
                .dealerId(dealerId)
                .amount(amount)
                .status(PaymentStatus.INITIATED)
                .transactionId(transactionId)
                .build();

        payment = paymentRepository.save(payment);

        try {
            String razorpayOrderId = razorpayGateway.createOrder(payment.getId(), orderId, amount);
            payment.setRazorpayOrderId(razorpayOrderId);
            payment = paymentRepository.save(payment);
            log.info("Razorpay order {} created for internalPaymentId={}", razorpayOrderId, payment.getId());
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for orderId={}: {}", orderId, e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("{\"error\":\"" + e.getMessage() + "\"}");
            paymentRepository.save(payment);
            return;
        }

        eventSourcingService.appendEvent(payment.getId(), PaymentEventType.PAYMENT_INITIATED,
                buildEventPayload(payment, "Auto-initiated from order.confirmed event"));

        try {
            eventPublisher.publishCheckoutReady(PaymentCheckoutReadyEvent.builder()
                    .orderId(orderId)
                    .paymentId(payment.getId())
                    .razorpayOrderId(payment.getRazorpayOrderId())
                    .amount(amount)
                    .dealerId(dealerId)
                    .farmerId(farmerId)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish payment.checkout.ready for paymentId={}: {}", payment.getId(), e.getMessage());
        }

        log.info("Payment {} initiated for orderId={}, awaiting frontend verification",
                payment.getId(), orderId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Payment findById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
    }

    private void assertParticipant(Payment payment, Long userId) {
        if (!payment.getFarmerId().equals(userId) && !payment.getDealerId().equals(userId)) {
            throw new UnauthorizedActionException("You are not a participant in this payment");
        }
    }

    private Map<String, Object> buildEventPayload(Payment payment, String note) {
        return Map.of(
                "paymentId",     payment.getId(),
                "orderId",       payment.getOrderId(),
                "farmerId",      payment.getFarmerId(),
                "dealerId",      payment.getDealerId(),
                "amount",        payment.getAmount(),
                "status",        payment.getStatus().name(),
                "transactionId", payment.getTransactionId() != null ? payment.getTransactionId() : "",
                "note",          note
        );
    }

    private String buildCropDetails(Long orderId, Double amount) {
        return String.format("{\"orderId\":%d,\"totalAmount\":%.2f}", orderId, amount);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
