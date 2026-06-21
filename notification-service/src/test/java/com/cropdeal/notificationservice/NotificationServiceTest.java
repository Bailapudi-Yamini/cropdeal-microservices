package com.cropdeal.notificationservice;

import com.cropdeal.notificationservice.consumer.*;
import com.cropdeal.notificationservice.entity.Notification;
import com.cropdeal.notificationservice.entity.NotificationType;
import com.cropdeal.notificationservice.event.*;
import com.cropdeal.notificationservice.service.EmailService;
import com.cropdeal.notificationservice.service.NotificationService;
import com.cropdeal.notificationservice.service.NotificationServiceImpl;
import com.cropdeal.notificationservice.dto.NotificationResponse;
import com.cropdeal.notificationservice.dto.PagedResponse;
import com.cropdeal.notificationservice.exception.UnauthorizedActionException;
import com.cropdeal.notificationservice.repository.NotificationRepository;
import com.cropdeal.notificationservice.service.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    // ── Service under test ────────────────────────────────────────────────────
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationMapper notificationMapper;
    @InjectMocks NotificationServiceImpl notificationService;

    // ── Consumer mocks ────────────────────────────────────────────────────────
    @Mock NotificationService mockNotificationService;
    @Mock EmailService emailService;

    private static final Long FARMER_ID = 1L;
    private static final Long DEALER_ID = 2L;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .userId(DEALER_ID)
                .title("Test")
                .message("Test message")
                .type(NotificationType.CROP_POSTED)
                .referenceId(10L)
                .read(false)
                .build();
        ReflectionTestUtils.setField(sampleNotification, "id", 1L);
    }

    // ── NotificationService ───────────────────────────────────────────────────

    @Test
    void createNotification_persistsCorrectly() {
        when(notificationRepository.save(any())).thenReturn(sampleNotification);

        Notification result = notificationService.createNotification(
                DEALER_ID, "Test", "Test message", NotificationType.CROP_POSTED, 10L);

        assertThat(result.getUserId()).isEqualTo(DEALER_ID);
        assertThat(result.isRead()).isFalse();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getMyNotifications_returnsPagedResponse() {
        NotificationResponse response = NotificationResponse.builder().id(1L).build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(DEALER_ID), any()))
                .thenReturn(new PageImpl<>(List.of(sampleNotification)));
        when(notificationRepository.countByUserIdAndReadFalse(DEALER_ID)).thenReturn(1L);
        when(notificationMapper.toResponse(sampleNotification)).thenReturn(response);

        PagedResponse<NotificationResponse> result = notificationService
                .getMyNotifications(DEALER_ID, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getUnreadCount()).isEqualTo(1L);
    }

    @Test
    void markAsRead_wrongOwner_throwsUnauthorized() {
        Notification otherUserNotif = Notification.builder()
                .userId(999L).title("X").message("X")
                .type(NotificationType.ORDER_PLACED).build();
        ReflectionTestUtils.setField(otherUserNotif, "id", 5L);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(otherUserNotif));

        assertThatThrownBy(() -> notificationService.markAsRead(5L, DEALER_ID))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void markAllAsRead_callsRepository() {
        when(notificationRepository.markAllAsRead(DEALER_ID)).thenReturn(3);
        notificationService.markAllAsRead(DEALER_ID);
        verify(notificationRepository).markAllAsRead(DEALER_ID);
    }

    // ── CropPostedConsumer ────────────────────────────────────────────────────

    @Test
    void cropPostedConsumer_fanOutToAllSubscribedDealers() {
        CropPostedConsumer consumer = new CropPostedConsumer(mockNotificationService, emailService);

        CropPostedEvent event = new CropPostedEvent();
        event.setListingId(100L);
        event.setCropName("Tomato");
        event.setCropType("VEGETABLE");
        event.setQuantityAvailable(200.0);
        event.setUnit("kg");
        event.setPricePerUnit(30.0);
        event.setLocation("Pune");
        event.setFarmerName("Ravi");
        event.setSubscribedDealerIds(List.of(10L, 20L, 30L));

        consumer.onCropPosted(event);

        // One notification per dealer
        verify(mockNotificationService, times(3))
                .createNotification(anyLong(), anyString(), anyString(),
                        eq(NotificationType.CROP_POSTED), eq(100L));
        verify(emailService, times(3)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void cropPostedConsumer_emptyDealerList_skipsNotifications() {
        CropPostedConsumer consumer = new CropPostedConsumer(mockNotificationService, emailService);

        CropPostedEvent event = new CropPostedEvent();
        event.setListingId(100L);
        event.setSubscribedDealerIds(List.of());

        consumer.onCropPosted(event);

        verifyNoInteractions(mockNotificationService, emailService);
    }

    // ── OrderPlacedConsumer ───────────────────────────────────────────────────

    @Test
    void orderPlacedConsumer_notifiesFarmer() {
        OrderPlacedConsumer consumer = new OrderPlacedConsumer(mockNotificationService, emailService);

        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId(50L);
        event.setFarmerId(FARMER_ID);
        event.setDealerId(DEALER_ID);
        event.setQuantity(100.0);
        event.setAgreedPricePerUnit(50.0);
        event.setTotalAmount(5000.0);

        consumer.onOrderPlaced(event);

        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockNotificationService).createNotification(
                userCaptor.capture(), anyString(), anyString(),
                eq(NotificationType.ORDER_PLACED), eq(50L));

        assertThat(userCaptor.getValue()).isEqualTo(FARMER_ID);
    }

    // ── NegotiationUpdateConsumer ─────────────────────────────────────────────

    @Test
    void negotiationUpdateConsumer_pendingStatus_notifiesRespondingParty() {
        NegotiationUpdateConsumer consumer = new NegotiationUpdateConsumer(mockNotificationService, emailService);

        NegotiationUpdateEvent event = new NegotiationUpdateEvent();
        event.setOrderId(50L);
        event.setFarmerId(FARMER_ID);
        event.setDealerId(DEALER_ID);
        event.setRespondingParty(FARMER_ID);  // dealer proposed, farmer responds
        event.setProposedPrice(80.0);
        event.setNegotiationStatus("PENDING");
        event.setRoundNumber(1);

        consumer.onNegotiationUpdate(event);

        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockNotificationService).createNotification(
                userCaptor.capture(), contains("Round 1"), anyString(),
                eq(NotificationType.NEGOTIATION_UPDATE), eq(50L));

        assertThat(userCaptor.getValue()).isEqualTo(FARMER_ID);
    }

    @Test
    void negotiationUpdateConsumer_acceptedStatus_confirmedMessage() {
        NegotiationUpdateConsumer consumer = new NegotiationUpdateConsumer(mockNotificationService, emailService);

        NegotiationUpdateEvent event = new NegotiationUpdateEvent();
        event.setOrderId(50L);
        event.setRespondingParty(DEALER_ID);
        event.setProposedPrice(80.0);
        event.setNegotiationStatus("ACCEPTED");
        event.setRoundNumber(2);

        consumer.onNegotiationUpdate(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockNotificationService).createNotification(
                eq(DEALER_ID), anyString(), messageCaptor.capture(),
                eq(NotificationType.NEGOTIATION_UPDATE), eq(50L));

        assertThat(messageCaptor.getValue()).contains("CONFIRMED");
    }

    // ── PaymentSuccessConsumer ────────────────────────────────────────────────

    @Test
    void paymentSuccessConsumer_notifiesBothFarmerAndDealer() {
        PaymentSuccessConsumer consumer = new PaymentSuccessConsumer(mockNotificationService, emailService);

        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setPaymentId(200L);
        event.setOrderId(50L);
        event.setFarmerId(FARMER_ID);
        event.setDealerId(DEALER_ID);
        event.setAmount(5000.0);
        event.setTransactionId("TXN-ABC123");
        event.setReceiptNumber("RCP-2024-000001");

        consumer.onPaymentSuccess(event);

        // Both farmer and dealer must be notified
        verify(mockNotificationService, times(2))
                .createNotification(anyLong(), anyString(), anyString(),
                        eq(NotificationType.PAYMENT_SUCCESS), eq(200L));

        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockNotificationService, times(2))
                .createNotification(userCaptor.capture(), anyString(), anyString(), any(), any());

        assertThat(userCaptor.getAllValues()).containsExactlyInAnyOrder(FARMER_ID, DEALER_ID);
    }

    // ── PaymentFailedConsumer ─────────────────────────────────────────────────

    @Test
    void paymentFailedConsumer_notifiesDealerOnly() {
        PaymentFailedConsumer consumer = new PaymentFailedConsumer(mockNotificationService, emailService);

        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setPaymentId(200L);
        event.setOrderId(50L);
        event.setFarmerId(FARMER_ID);
        event.setDealerId(DEALER_ID);
        event.setAmount(5000.0);
        event.setTransactionId("TXN-FAIL");
        event.setFailureReason("INSUFFICIENT_FUNDS");

        consumer.onPaymentFailed(event);

        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockNotificationService).createNotification(
                userCaptor.capture(), anyString(), anyString(),
                eq(NotificationType.PAYMENT_FAILED), eq(200L));

        // Only dealer is notified for payment failure
        assertThat(userCaptor.getValue()).isEqualTo(DEALER_ID);
        verify(mockNotificationService, times(1))
                .createNotification(any(), any(), any(), any(), any());
    }
}
