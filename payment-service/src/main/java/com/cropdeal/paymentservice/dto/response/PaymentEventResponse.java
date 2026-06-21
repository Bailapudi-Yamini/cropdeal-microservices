package com.cropdeal.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentEventResponse {
    private Long id;
    private Long paymentId;
    private String eventType;
    private String payload;
    private LocalDateTime occurredAt;
}
