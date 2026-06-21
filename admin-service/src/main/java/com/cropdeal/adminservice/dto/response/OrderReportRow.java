package com.cropdeal.adminservice.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderReportRow {
    private Long orderId;
    private Long farmerId;
    private Long dealerId;
    private String cropName;
    private String cropType;
    private Double quantity;
    private Double agreedPricePerUnit;
    private Double totalAmount;
    private String orderStatus;
    private String location;
    private LocalDateTime placedAt;
    private LocalDateTime completedAt;
}
