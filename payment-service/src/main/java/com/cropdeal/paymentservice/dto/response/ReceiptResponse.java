package com.cropdeal.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReceiptResponse {
    private Long id;
    private Long paymentId;
    private Long farmerId;
    private Long dealerId;
    private String receiptNumber;
    private Double amount;
    private String cropDetails;
    private LocalDateTime generatedAt;
}
