package com.cropdeal.adminservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummary {
    private long totalOrders;
    private long completedOrders;
    private long pendingOrders;
    private long cancelledOrders;
    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private Double totalRevenue;
}
