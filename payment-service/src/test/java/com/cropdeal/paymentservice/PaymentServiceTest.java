package com.cropdeal.paymentservice;

import com.cropdeal.paymentservice.dto.request.InitiatePaymentRequest;
import com.cropdeal.paymentservice.dto.request.RefundRequest;
import com.cropdeal.paymentservice.dto.response.PaymentResponse;
import com.cropdeal.paymentservice.entity.*;
import com.cropdeal.paymentservice.event.PaymentFailedEvent;
import com.cropdeal.paymentservice.event.PaymentSuccessEvent;
import com.cropdeal.paymentservice.exception.DuplicatePaymentException;
import com.cropdeal.paymentservice.exception.InvalidPaymentStateException;
import com.cropdeal.paymentservice.exception.ResourceNotFoundException;
import com.cropdeal.paymentservice.exception.UnauthorizedActionException;
import com.cropdeal.paymentservice.repository.PaymentRepository;
import com.cropdeal.paymentservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock EventSourcingService eventSourcingService;
    @Mock ReceiptService receiptService;
    @Mock EventPublisher eventPublisher;
    @Mock PaymentMapper paymentMapper;

    @InjectMocks PaymentServiceImpl paymentService;

    private static final Long FARMER_ID  = 1L;
    private static final Long DEALER_ID  = 2L;
    private static final Long ORDER_ID   = 10L;
    private static final Long PAYMENT_ID = 100L;

    private Payment successfulPayment;
    private Payment initiatedPayment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "gatewaySuccessRate", 1.0); // force success

        initiatedPayment = Payment.builder()
                .orderId(ORDER_ID).farmerId(FARMER_ID).dealerId(DEALER_ID)
                .amount(5000.0).status(PaymentStatus.INITIATED)
                .transactionId("TXN-TEST123").build();
        ReflectionTestUtils.setField(initiatedPayment, "id", PAYMENT_ID);

        successfulPayment = Payment.builder()
                .orderId(ORDER_ID).farmerId(FARMER_ID).dealerId(DEALER_ID)
                .amount(5000.0).status(PaymentStatus.SUCCESS)
                .transactionId("TXN-TEST123").paymentGatewayRef("GW-ABC123")
                .paidAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(successfulPayment, "id", PAYMENT_ID);
    }

    // ── initiatePayment ───────────────────────────────────────────────────────

    @Test
    void initiatePayment_success_appendsInitiatedAndSuccessEvents() {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderId(ORDER_ID);
        request.setAmount(5000.0);

        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentRepository.save(any())).thenReturn(initiatedPayment);
        when(paymentRepository.markSuccess(any(), any(), any())).thenReturn(1);
        when(paymentMapper.toPaymentResponse(any())).thenReturn(
                PaymentResponse.builder().id(PAYMENT_ID).status(PaymentStatus.SUCCESS).build());

        PaymentResponse result = paymentService.initiatePayment(DEALER_ID, request);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // Verify Event Sourcing: INITIATED + SUCCESS events appended
        verify(eventSourcingService, times(2)).appendEvent(eq(PAYMENT_ID), anyString(), any());

        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventSourcingService, times(2))
                .appendEvent(eq(PAYMENT_ID), eventTypeCaptor.capture(), any());

        assertThat(eventTypeCaptor.getAllValues())
                .containsExactly(PaymentEventType.PAYMENT_INITIATED, PaymentEventType.PAYMENT_SUCCESS);
    }

    @Test
    void initiatePayment_gatewayFails_appendsFailedEventAndPublishesFailedEvent() {
        ReflectionTestUtils.setField(paymentService, "gatewaySuccessRate", 0.0); // force failure

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderId(ORDER_ID);
        request.setAmount(5000.0);

        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentRepository.save(any())).thenReturn(initiatedPayment);
        when(paymentRepository.updateStatusWithResponse(any(), any(), any())).thenReturn(1);
        when(paymentMapper.toPaymentResponse(any())).thenReturn(
                PaymentResponse.builder().id(PAYMENT_ID).status(PaymentStatus.FAILED).build());

        PaymentResponse result = paymentService.initiatePayment(DEALER_ID, request);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);

        // Verify INITIATED + FAILED events
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(eventSourcingService, times(2)).appendEvent(eq(PAYMENT_ID), captor.capture(), any());
        assertThat(captor.getAllValues())
                .containsExactly(PaymentEventType.PAYMENT_INITIATED, PaymentEventType.PAYMENT_FAILED);

        verify(eventPublisher).publishPaymentFailed(any(PaymentFailedEvent.class));
        verify(eventPublisher, never()).publishPaymentSuccess(any());
    }

    @Test
    void initiatePayment_duplicate_throwsDuplicatePaymentException() {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderId(ORDER_ID);
        request.setAmount(5000.0);

        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.initiatePayment(DEALER_ID, request))
                .isInstanceOf(DuplicatePaymentException.class)
                .hasMessageContaining("Payment already exists");

        verifyNoInteractions(eventSourcingService, eventPublisher);
    }

    // ── handleOrderConfirmed ──────────────────────────────────────────────────

    @Test
    void handleOrderConfirmed_success_generatesReceiptAndPublishesSuccessEvent() {
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentRepository.save(any())).thenReturn(initiatedPayment);
        when(paymentRepository.markSuccess(any(), any(), any())).thenReturn(1);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(successfulPayment));
        when(paymentMapper.toPaymentResponse(any())).thenReturn(
                PaymentResponse.builder().id(PAYMENT_ID).status(PaymentStatus.SUCCESS).build());

        Receipt mockReceipt = Receipt.builder()
                .paymentId(PAYMENT_ID).receiptNumber("RCP-2024-000001")
                .farmerId(FARMER_ID).dealerId(DEALER_ID).amount(5000.0)
                .cropDetails("{}").build();
        when(receiptService.generateReceipt(any(), any())).thenReturn(mockReceipt);

        paymentService.handleOrderConfirmed(ORDER_ID, FARMER_ID, DEALER_ID, 100L, 5000.0);

        verify(receiptService).generateReceipt(any(Payment.class), anyString());

        ArgumentCaptor<PaymentSuccessEvent> captor = ArgumentCaptor.forClass(PaymentSuccessEvent.class);
        verify(eventPublisher).publishPaymentSuccess(captor.capture());
        PaymentSuccessEvent event = captor.getValue();
        assertThat(event.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(event.getFarmerId()).isEqualTo(FARMER_ID);
        assertThat(event.getDealerId()).isEqualTo(DEALER_ID);
        assertThat(event.getReceiptNumber()).isEqualTo("RCP-2024-000001");
    }

    @Test
    void handleOrderConfirmed_alreadyExists_skipsProcessing() {
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

        paymentService.handleOrderConfirmed(ORDER_ID, FARMER_ID, DEALER_ID, 100L, 5000.0);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventSourcingService, eventPublisher, receiptService);
    }

    // ── processRefund ─────────────────────────────────────────────────────────

    @Test
    void processRefund_success_appendsRefundedEvent() {
        RefundRequest request = new RefundRequest();
        request.setReason("Crop quality issue");

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(successfulPayment));
        when(paymentRepository.save(any())).thenReturn(successfulPayment);
        when(paymentMapper.toPaymentResponse(any())).thenReturn(
                PaymentResponse.builder().id(PAYMENT_ID).status(PaymentStatus.REFUNDED).build());

        PaymentResponse result = paymentService.processRefund(PAYMENT_ID, request);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(eventSourcingService).appendEvent(eq(PAYMENT_ID),
                eq(PaymentEventType.PAYMENT_REFUNDED), any());
    }

    @Test
    void processRefund_notSuccessful_throwsInvalidState() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(initiatedPayment));

        assertThatThrownBy(() -> paymentService.processRefund(PAYMENT_ID, new RefundRequest()))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("Only successful payments");
    }

    // ── getPaymentById ────────────────────────────────────────────────────────

    @Test
    void getPaymentById_nonParticipant_throwsUnauthorized() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(successfulPayment));

        assertThatThrownBy(() -> paymentService.getPaymentById(PAYMENT_ID, 999L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void getPaymentById_notFound_throwsResourceNotFound() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(PAYMENT_ID, DEALER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
