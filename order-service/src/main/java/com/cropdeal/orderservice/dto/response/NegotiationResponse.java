package com.cropdeal.orderservice.dto.response;

import com.cropdeal.orderservice.entity.NegotiationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NegotiationResponse {
    private Long id;
    private Long orderId;
    private Long initiatedBy;
    private String proposedBy;      // "FARMER" or "DEALER" — derived from initiatedBy vs order.farmerId
    private Double proposedPrice;
    private String message;
    private NegotiationStatus status;
    private LocalDateTime createdAt;
}
