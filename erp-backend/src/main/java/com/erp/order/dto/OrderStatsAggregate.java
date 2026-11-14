package com.erp.order.dto;

import java.math.BigDecimal;

public record OrderStatsAggregate(
    Long totalOrders,
    Long pendingOrders,
    Long confirmedOrders,
    Long shippingOrders,
    Long completedOrders,
    Long cancelledOrders,
    Long todayOrders,
    BigDecimal activeRevenue,
    BigDecimal completedRevenue,
    BigDecimal paidRevenue
) {
    public OrderStatsAggregate {
        totalOrders = totalOrders != null ? totalOrders : 0L;
        pendingOrders = pendingOrders != null ? pendingOrders : 0L;
        confirmedOrders = confirmedOrders != null ? confirmedOrders : 0L;
        shippingOrders = shippingOrders != null ? shippingOrders : 0L;
        completedOrders = completedOrders != null ? completedOrders : 0L;
        cancelledOrders = cancelledOrders != null ? cancelledOrders : 0L;
        todayOrders = todayOrders != null ? todayOrders : 0L;
        activeRevenue = activeRevenue != null ? activeRevenue : BigDecimal.ZERO;
        completedRevenue = completedRevenue != null ? completedRevenue : BigDecimal.ZERO;
        paidRevenue = paidRevenue != null ? paidRevenue : BigDecimal.ZERO;
    }
}
