package com.cropdeal.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent implements Serializable {
    private Long orderId;
    private Long cropListingId;
    private Long farmerId;
    private Long dealerId;
    private Double totalAmount;
}
