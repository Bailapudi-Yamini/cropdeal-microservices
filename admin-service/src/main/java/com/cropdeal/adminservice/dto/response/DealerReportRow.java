package com.cropdeal.adminservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DealerReportRow {
    private Long dealerId;
    private long totalOrders;
    private long completedOrders;
    private long cancelledOrders;
    private Double totalRevenue;
    private Double averageOrderValue;
}
