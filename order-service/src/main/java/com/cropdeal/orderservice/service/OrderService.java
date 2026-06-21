package com.cropdeal.orderservice.service;

import com.cropdeal.orderservice.dto.request.NegotiationRequest;
import com.cropdeal.orderservice.dto.request.PlaceOrderRequest;
import com.cropdeal.orderservice.dto.response.NegotiationResponse;
import com.cropdeal.orderservice.dto.response.OrderResponse;
import com.cropdeal.orderservice.dto.response.PagedResponse;
import com.cropdeal.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    // ── Order lifecycle ───────────────────────────────────────────────────────
    OrderResponse placeOrder(Long dealerId, PlaceOrderRequest request);

    OrderResponse getOrderById(Long orderId, Long callerId);

    PagedResponse<OrderResponse> getOrdersForDealer(Long dealerId, OrderStatus status, Pageable pageable);

    PagedResponse<OrderResponse> getOrdersForFarmer(Long farmerId, OrderStatus status, Pageable pageable);

    void cancelOrder(Long orderId, Long dealerId);

    OrderResponse confirmOrder(Long orderId, Long farmerId);

    // ── Negotiation flow ──────────────────────────────────────────────────────

    /** Dealer or farmer proposes / counters a price */
    NegotiationResponse proposeNegotiation(Long orderId, Long initiatorId, NegotiationRequest request);

    /** The other party accepts the latest pending negotiation */
    OrderResponse acceptNegotiation(Long orderId, Long responderId);

    /** The other party rejects the latest pending negotiation */
    OrderResponse rejectNegotiation(Long orderId, Long responderId);

    List<NegotiationResponse> getNegotiationHistory(Long orderId, Long callerId);

    // ── Internal (called by EventConsumer) ───────────────────────────────────
    void markOrderCompleted(Long orderId);
}
