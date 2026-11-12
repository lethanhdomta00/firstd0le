package com.erp.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderStatsDto {
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long shippingOrders;
    private long completedOrders;
    private long cancelledOrders;
    private long todayOrders;
    private long pendingFulfillmentCount;
    private BigDecimal activeRevenue;
    private BigDecimal completedRevenue;
    private BigDecimal paidRevenue;
    private BigDecimal avgOrderValue;
    private double completionRate;
}
