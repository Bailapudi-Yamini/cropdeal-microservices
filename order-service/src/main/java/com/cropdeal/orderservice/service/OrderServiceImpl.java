package com.cropdeal.orderservice.service;

import com.cropdeal.orderservice.dto.request.NegotiationRequest;
import com.cropdeal.orderservice.dto.request.PlaceOrderRequest;
import com.cropdeal.orderservice.dto.response.NegotiationResponse;
import com.cropdeal.orderservice.dto.response.OrderResponse;
import com.cropdeal.orderservice.dto.response.PagedResponse;
import com.cropdeal.orderservice.entity.*;
import com.cropdeal.orderservice.event.NegotiationUpdateEvent;
import com.cropdeal.orderservice.event.OrderConfirmedEvent;
import com.cropdeal.orderservice.event.OrderPlacedEvent;
import com.cropdeal.orderservice.exception.InvalidOrderStateException;
import com.cropdeal.orderservice.exception.ResourceNotFoundException;
import com.cropdeal.orderservice.exception.UnauthorizedActionException;
import com.cropdeal.orderservice.repository.NegotiationRepository;
import com.cropdeal.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final NegotiationRepository negotiationRepository;
    private final OrderMapper orderMapper;
    private final EventPublisher eventPublisher;
    private final CropServiceClient cropServiceClient;

    @Value("${order.negotiation.max-rounds:5}")
    private int maxNegotiationRounds;

    // ── Order lifecycle ───────────────────────────────────────────────────────

    @Override
    public OrderResponse placeOrder(Long dealerId, PlaceOrderRequest request) {
        // NOTE: In a full implementation, crop-service would be called via Feign
        // to validate the listing exists and retrieve pricePerUnit.
        // Here we use the listing ID and a placeholder price — the Feign client
        // integration is wired in CropServiceClient (stub shown below).
        double pricePerUnit = fetchListingPrice(request.getCropListingId());
        double total        = request.getQuantity() * pricePerUnit;

        Order order = Order.builder()
                .cropListingId(request.getCropListingId())
                .farmerId(resolveFarmerId(request.getCropListingId()))
                .dealerId(dealerId)
                .quantity(request.getQuantity())
                .agreedPricePerUnit(pricePerUnit)
                .totalAmount(total)
                .dealerNotes(request.getDealerNotes())
                .status(OrderStatus.PENDING)
                .negotiationRounds(0)
                .build();

        order = orderRepository.save(order);

        try {
            eventPublisher.publishOrderPlaced(OrderPlacedEvent.builder()
                    .orderId(order.getId())
                    .cropListingId(order.getCropListingId())
                    .farmerId(order.getFarmerId())
                    .dealerId(order.getDealerId())
                    .quantity(order.getQuantity())
                    .agreedPricePerUnit(order.getAgreedPricePerUnit())
                    .totalAmount(order.getTotalAmount())
                    .dealerNotes(order.getDealerNotes())
                    .placedAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish order.placed event for orderId={}: {}", order.getId(), e.getMessage());
        }

        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long callerId) {
        Order order = findOrderById(orderId);
        assertParticipant(order, callerId);
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersForDealer(Long dealerId, OrderStatus status,
                                                            Pageable pageable) {
        Page<Order> page = (status != null)
                ? orderRepository.findByDealerIdAndStatus(dealerId, status, pageable)
                : orderRepository.findByDealerId(dealerId, pageable);
        return toPagedResponse(page.map(orderMapper::toOrderResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersForFarmer(Long farmerId, OrderStatus status,
                                                            Pageable pageable) {
        Page<Order> page = (status != null)
                ? orderRepository.findByFarmerIdAndStatus(farmerId, status, pageable)
                : orderRepository.findByFarmerId(farmerId, pageable);
        return toPagedResponse(page.map(orderMapper::toOrderResponse));
    }

    @Override
    public OrderResponse confirmOrder(Long orderId, Long farmerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getFarmerId().equals(farmerId)) {
            throw new UnauthorizedActionException("You are not the farmer for this order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only PENDING orders can be confirmed. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        try {
            eventPublisher.publishOrderConfirmed(OrderConfirmedEvent.builder()
                    .orderId(order.getId())
                    .cropListingId(order.getCropListingId())
                    .farmerId(order.getFarmerId())
                    .dealerId(order.getDealerId())
                    .totalAmount(order.getTotalAmount())
                    .build());
            log.info("order.confirmed event published for orderId={}", orderId);
        } catch (Exception e) {
            log.error("FAILED to publish order.confirmed for orderId={}: {} — {}",
                    orderId, e.getClass().getSimpleName(), e.getMessage(), e);
        }

        log.info("Order {} confirmed by farmer {}", orderId, farmerId);
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public void cancelOrder(Long orderId, Long dealerId) {
        Order order = orderRepository.findByIdAndDealerId(orderId, dealerId)
                .orElseThrow(() -> new UnauthorizedActionException(
                        "Order not found or you are not the dealer for this order"));

        assertCancellable(order);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Close any open negotiation rounds
        negotiationRepository.closeAllPendingForOrder(orderId, NegotiationStatus.REJECTED);

        log.info("Order {} cancelled by dealer {}", orderId, dealerId);
    }

    // ── Negotiation flow ──────────────────────────────────────────────────────

    @Override
    public NegotiationResponse proposeNegotiation(Long orderId, Long initiatorId,
                                                   NegotiationRequest request) {
        Order order = findOrderById(orderId);
        assertParticipant(order, initiatorId);
        assertNegotiable(order);

        // Enforce max rounds
        if (order.getNegotiationRounds() >= maxNegotiationRounds) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new InvalidOrderStateException(
                    "Maximum negotiation rounds (" + maxNegotiationRounds + ") reached. Order cancelled.");
        }

        // Close any existing PENDING round before opening a new one
        negotiationRepository.closeAllPendingForOrder(orderId, NegotiationStatus.COUNTERED);

        Negotiation negotiation = Negotiation.builder()
                .order(order)
                .initiatedBy(initiatorId)
                .proposedPrice(request.getProposedPrice())
                .message(request.getMessage())
                .status(NegotiationStatus.PENDING)
                .build();

        negotiation = negotiationRepository.save(negotiation);

        // Increment round counter and move order to NEGOTIATING
        order.setNegotiationRounds(order.getNegotiationRounds() + 1);
        order.setStatus(OrderStatus.NEGOTIATING);
        orderRepository.save(order);

        Long respondingParty = resolveRespondingParty(order, initiatorId);

        eventPublisher.publishNegotiationUpdate(NegotiationUpdateEvent.builder()
                .negotiationId(negotiation.getId())
                .orderId(orderId)
                .farmerId(order.getFarmerId())
                .dealerId(order.getDealerId())
                .initiatedBy(initiatorId)
                .respondingParty(respondingParty)
                .proposedPrice(request.getProposedPrice())
                .message(request.getMessage())
                .negotiationStatus(NegotiationStatus.PENDING)
                .orderStatus(OrderStatus.NEGOTIATING)
                .roundNumber(order.getNegotiationRounds())
                .occurredAt(LocalDateTime.now())
                .build());

        return orderMapper.toNegotiationResponse(negotiation);
    }

    @Override
    public OrderResponse acceptNegotiation(Long orderId, Long responderId) {
        Order order = findOrderById(orderId);
        assertParticipant(order, responderId);

        Negotiation pending = findPendingNegotiation(orderId);

        // Responder must be the OTHER party — not the one who proposed this round
        if (pending.getInitiatedBy().equals(responderId)) {
            throw new UnauthorizedActionException("You cannot accept your own negotiation proposal");
        }

        // Validate by role: farmer accepts dealer's proposal, dealer accepts farmer's proposal
        boolean isFarmer = order.getFarmerId().equals(responderId);
        boolean isDealer = order.getDealerId().equals(responderId);
        boolean proposedByFarmer = order.getFarmerId().equals(pending.getInitiatedBy());
        boolean proposedByDealer = order.getDealerId().equals(pending.getInitiatedBy());

        if (isFarmer && !proposedByDealer) {
            throw new UnauthorizedActionException("Farmer can only accept a proposal made by the dealer");
        }
        if (isDealer && !proposedByFarmer) {
            throw new UnauthorizedActionException("Dealer can only accept a proposal made by the farmer");
        }

        // Apply the agreed price
        negotiationRepository.updateStatus(pending.getId(), NegotiationStatus.ACCEPTED);
        order.setAgreedPricePerUnit(pending.getProposedPrice());
        order.setTotalAmount(order.getQuantity() * pending.getProposedPrice());
        order.setStatus(OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        Long respondingParty = resolveRespondingParty(order, responderId);

        eventPublisher.publishNegotiationUpdate(NegotiationUpdateEvent.builder()
                .negotiationId(pending.getId())
                .orderId(orderId)
                .farmerId(order.getFarmerId())
                .dealerId(order.getDealerId())
                .initiatedBy(responderId)
                .respondingParty(respondingParty)
                .proposedPrice(pending.getProposedPrice())
                .negotiationStatus(NegotiationStatus.ACCEPTED)
                .orderStatus(OrderStatus.CONFIRMED)
                .roundNumber(order.getNegotiationRounds())
                .occurredAt(LocalDateTime.now())
                .build());

        // Trigger payment flow
        eventPublisher.publishOrderConfirmed(OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .cropListingId(order.getCropListingId())
                .farmerId(order.getFarmerId())
                .dealerId(order.getDealerId())
                .totalAmount(order.getTotalAmount())
                .build());

        return orderMapper.toOrderResponse(order);
    }

    @Override
    public OrderResponse rejectNegotiation(Long orderId, Long responderId) {
        Order order = findOrderById(orderId);
        assertParticipant(order, responderId);

        // Either participant can reject — close any open negotiation round if one exists
        Negotiation pending = negotiationRepository
                .findTopByOrderIdAndStatusOrderByCreatedAtDesc(orderId, NegotiationStatus.PENDING)
                .orElse(null);
        if (pending != null) {
            negotiationRepository.updateStatus(pending.getId(), NegotiationStatus.REJECTED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        Long respondingParty = resolveRespondingParty(order, responderId);

        eventPublisher.publishNegotiationUpdate(NegotiationUpdateEvent.builder()
                .negotiationId(pending != null ? pending.getId() : null)
                .orderId(orderId)
                .farmerId(order.getFarmerId())
                .dealerId(order.getDealerId())
                .initiatedBy(responderId)
                .respondingParty(respondingParty)
                .proposedPrice(pending != null ? pending.getProposedPrice() : null)
                .negotiationStatus(NegotiationStatus.REJECTED)
                .orderStatus(OrderStatus.CANCELLED)
                .roundNumber(order.getNegotiationRounds())
                .occurredAt(LocalDateTime.now())
                .build());

        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NegotiationResponse> getNegotiationHistory(Long orderId, Long callerId) {
        Order order = findOrderById(orderId);
        assertParticipant(order, callerId);
        return negotiationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(orderMapper::toNegotiationResponse)
                .toList();
    }

    @Override
    public void markOrderCompleted(Long orderId) {
        int updated = orderRepository.updateStatus(orderId, OrderStatus.COMPLETED);
        if (updated == 0) {
            log.warn("markOrderCompleted: order {} not found", orderId);
        } else {
            log.info("Order {} marked as COMPLETED", orderId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private Negotiation findPendingNegotiation(Long orderId) {
        return negotiationRepository
                .findTopByOrderIdAndStatusOrderByCreatedAtDesc(orderId, NegotiationStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending negotiation found for order: " + orderId));
    }

    private void assertParticipant(Order order, Long userId) {
        if (!order.getFarmerId().equals(userId) && !order.getDealerId().equals(userId)) {
            throw new UnauthorizedActionException("You are not a participant in this order");
        }
    }

    private void assertNegotiable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.NEGOTIATING) {
            throw new InvalidOrderStateException(
                    "Order cannot be negotiated in status: " + order.getStatus());
        }
    }

    private void assertCancellable(Order order) {
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                    "Order cannot be cancelled in status: " + order.getStatus());
        }
    }

    /** Returns the party who should be notified — the one who did NOT initiate this action */
    private Long resolveRespondingParty(Order order, Long initiatorId) {
        return initiatorId.equals(order.getDealerId()) ? order.getFarmerId() : order.getDealerId();
    }

    private double fetchListingPrice(Long listingId) {
        return cropServiceClient.getListingById(listingId).data().pricePerUnit();
    }

    private Long resolveFarmerId(Long listingId) {
        return cropServiceClient.getListingById(listingId).data().farmerId();
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
