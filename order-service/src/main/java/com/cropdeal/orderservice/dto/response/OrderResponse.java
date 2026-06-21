package com.cropdeal.orderservice.dto.response;

import com.cropdeal.orderservice.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private Long cropListingId;
    private Long farmerId;
    private Long dealerId;
    private Double quantity;
    private Double agreedPricePerUnit;
    private Double totalAmount;
    private OrderStatus status;
    private String dealerNotes;
    private int negotiationRounds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
