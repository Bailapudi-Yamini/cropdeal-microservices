package com.cropdeal.adminservice.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentReportRow {
    private Long paymentId;
    private Long orderId;
    private Long farmerId;
    private Long dealerId;
    private Double amount;
    private String paymentStatus;
    private String transactionId;
    private String receiptNumber;
    private LocalDateTime paidAt;
}
