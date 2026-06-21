package com.cropdeal.orderservice;

import com.cropdeal.orderservice.dto.request.NegotiationRequest;
import com.cropdeal.orderservice.dto.request.PlaceOrderRequest;
import com.cropdeal.orderservice.dto.response.NegotiationResponse;
import com.cropdeal.orderservice.dto.response.OrderResponse;
import com.cropdeal.orderservice.entity.*;
import com.cropdeal.orderservice.event.NegotiationUpdateEvent;
import com.cropdeal.orderservice.event.OrderConfirmedEvent;
import com.cropdeal.orderservice.event.OrderPlacedEvent;
import com.cropdeal.orderservice.exception.InvalidOrderStateException;
import com.cropdeal.orderservice.exception.ResourceNotFoundException;
import com.cropdeal.orderservice.exception.UnauthorizedActionException;
import com.cropdeal.orderservice.repository.NegotiationRepository;
import com.cropdeal.orderservice.repository.OrderRepository;
import com.cropdeal.orderservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock NegotiationRepository negotiationRepository;
    @Mock OrderMapper orderMapper;
    @Mock EventPublisher eventPublisher;
    @Mock CropServiceClient cropServiceClient;

    @InjectMocks OrderServiceImpl orderService;

    private static final Long FARMER_ID = 1L;
    private static final Long DEALER_ID = 2L;
    private static final Long ORDER_ID  = 10L;

    private Order pendingOrder;
    private Order negotiatingOrder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "maxNegotiationRounds", 5);

        pendingOrder = Order.builder()
                .farmerId(FARMER_ID)
                .dealerId(DEALER_ID)
                .cropListingId(100L)
                .quantity(50.0)
                .agreedPricePerUnit(100.0)
                .totalAmount(5000.0)
                .status(OrderStatus.PENDING)
                .negotiationRounds(0)
                .build();
        ReflectionTestUtils.setField(pendingOrder, "id", ORDER_ID);

        negotiatingOrder = Order.builder()
                .farmerId(FARMER_ID)
                .dealerId(DEALER_ID)
                .cropListingId(100L)
                .quantity(50.0)
                .agreedPricePerUnit(100.0)
                .totalAmount(5000.0)
                .status(OrderStatus.NEGOTIATING)
                .negotiationRounds(1)
                .build();
        ReflectionTestUtils.setField(negotiatingOrder, "id", ORDER_ID);
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_success_publishesOrderPlacedEvent() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCropListingId(100L);
        request.setQuantity(50.0);

        var summary = new CropServiceClient.CropListingSummary(100L, FARMER_ID, 100.0, "AVAILABLE");
        var wrapper = new CropServiceClient.ApiWrapper(true, "success", summary);
        when(cropServiceClient.getListingById(100L)).thenReturn(wrapper);
        when(orderRepository.save(any())).thenReturn(pendingOrder);
        when(orderMapper.toOrderResponse(pendingOrder)).thenReturn(OrderResponse.builder().id(ORDER_ID).build());

        OrderResponse result = orderService.placeOrder(DEALER_ID, request);

        assertThat(result.getId()).isEqualTo(ORDER_ID);
        ArgumentCaptor<OrderPlacedEvent> captor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(eventPublisher).publishOrderPlaced(captor.capture());
        assertThat(captor.getValue().getDealerId()).isEqualTo(DEALER_ID);
        assertThat(captor.getValue().getFarmerId()).isEqualTo(FARMER_ID);
    }

    // ── proposeNegotiation ────────────────────────────────────────────────────

    @Test
    void proposeNegotiation_byDealer_publishesNegotiationUpdateEvent() {
        NegotiationRequest request = new NegotiationRequest();
        request.setProposedPrice(90.0);
        request.setMessage("Can you lower the price?");

        Negotiation savedNeg = Negotiation.builder()
                .order(pendingOrder)
                .initiatedBy(DEALER_ID)
                .proposedPrice(90.0)
                .status(NegotiationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(savedNeg, "id", 1L);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
        when(negotiationRepository.save(any())).thenReturn(savedNeg);
        when(orderRepository.save(any())).thenReturn(pendingOrder);
        when(orderMapper.toNegotiationResponse(savedNeg))
                .thenReturn(NegotiationResponse.builder().id(1L).proposedPrice(90.0).build());

        NegotiationResponse result = orderService.proposeNegotiation(ORDER_ID, DEALER_ID, request);

        assertThat(result.getProposedPrice()).isEqualTo(90.0);

        ArgumentCaptor<NegotiationUpdateEvent> captor = ArgumentCaptor.forClass(NegotiationUpdateEvent.class);
        verify(eventPublisher).publishNegotiationUpdate(captor.capture());
        NegotiationUpdateEvent event = captor.getValue();
        assertThat(event.getInitiatedBy()).isEqualTo(DEALER_ID);
        assertThat(event.getRespondingParty()).isEqualTo(FARMER_ID); // farmer should be notified
        assertThat(event.getNegotiationStatus()).isEqualTo(NegotiationStatus.PENDING);
    }

    @Test
    void proposeNegotiation_maxRoundsExceeded_cancelsOrderAndThrows() {
        ReflectionTestUtils.setField(negotiatingOrder, "negotiationRounds", 5);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(negotiatingOrder));
        when(orderRepository.save(any())).thenReturn(negotiatingOrder);

        assertThatThrownBy(() -> orderService.proposeNegotiation(
                ORDER_ID, DEALER_ID, new NegotiationRequest()))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Maximum negotiation rounds");

        assertThat(negotiatingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void proposeNegotiation_nonParticipant_throwsUnauthorized() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.proposeNegotiation(
                ORDER_ID, 999L, new NegotiationRequest()))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    // ── acceptNegotiation ─────────────────────────────────────────────────────

    @Test
    void acceptNegotiation_byFarmer_confirmsOrderAndPublishesTwoEvents() {
        Negotiation pending = Negotiation.builder()
                .order(negotiatingOrder)
                .initiatedBy(DEALER_ID)   // dealer proposed
                .proposedPrice(90.0)
                .status(NegotiationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(pending, "id", 1L);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(negotiatingOrder));
        when(negotiationRepository.findTopByOrderIdAndStatusOrderByCreatedAtDesc(ORDER_ID, NegotiationStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(orderRepository.save(any())).thenReturn(negotiatingOrder);
        when(orderMapper.toOrderResponse(any())).thenReturn(OrderResponse.builder().status(OrderStatus.CONFIRMED).build());

        OrderResponse result = orderService.acceptNegotiation(ORDER_ID, FARMER_ID);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(eventPublisher).publishNegotiationUpdate(any(NegotiationUpdateEvent.class));
        verify(eventPublisher).publishOrderConfirmed(any(OrderConfirmedEvent.class));
        assertThat(negotiatingOrder.getAgreedPricePerUnit()).isEqualTo(90.0);
        assertThat(negotiatingOrder.getTotalAmount()).isEqualTo(50.0 * 90.0);
    }

    @Test
    void acceptNegotiation_byProposer_throwsUnauthorized() {
        Negotiation pending = Negotiation.builder()
                .order(negotiatingOrder)
                .initiatedBy(DEALER_ID)
                .proposedPrice(90.0)
                .status(NegotiationStatus.PENDING)
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(negotiatingOrder));
        when(negotiationRepository.findTopByOrderIdAndStatusOrderByCreatedAtDesc(ORDER_ID, NegotiationStatus.PENDING))
                .thenReturn(Optional.of(pending));

        // Dealer tries to accept their own proposal
        assertThatThrownBy(() -> orderService.acceptNegotiation(ORDER_ID, DEALER_ID))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("cannot accept your own");
    }

    // ── rejectNegotiation ─────────────────────────────────────────────────────

    @Test
    void rejectNegotiation_cancelsOrderAndPublishesEvent() {
        Negotiation pending = Negotiation.builder()
                .order(negotiatingOrder)
                .initiatedBy(DEALER_ID)
                .proposedPrice(90.0)
                .status(NegotiationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(pending, "id", 1L);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(negotiatingOrder));
        when(negotiationRepository.findTopByOrderIdAndStatusOrderByCreatedAtDesc(ORDER_ID, NegotiationStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(orderRepository.save(any())).thenReturn(negotiatingOrder);
        when(orderMapper.toOrderResponse(any())).thenReturn(OrderResponse.builder().status(OrderStatus.CANCELLED).build());

        OrderResponse result = orderService.rejectNegotiation(ORDER_ID, FARMER_ID);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventPublisher).publishNegotiationUpdate(any(NegotiationUpdateEvent.class));
        verify(eventPublisher, never()).publishOrderConfirmed(any());
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Test
    void cancelOrder_completedOrder_throwsInvalidState() {
        Order completedOrder = Order.builder()
                .farmerId(FARMER_ID).dealerId(DEALER_ID)
                .status(OrderStatus.COMPLETED).negotiationRounds(0)
                .quantity(10.0).agreedPricePerUnit(100.0).totalAmount(1000.0)
                .cropListingId(1L).build();

        when(orderRepository.findByIdAndDealerId(ORDER_ID, DEALER_ID))
                .thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, DEALER_ID))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    // ── getOrderById ──────────────────────────────────────────────────────────

    @Test
    void getOrderById_notFound_throwsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById(99L, DEALER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
