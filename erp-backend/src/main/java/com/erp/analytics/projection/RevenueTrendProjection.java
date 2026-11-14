package com.erp.analytics.projection;

import java.math.BigDecimal;

public interface RevenueTrendProjection {
    String getOrderDate();
    Long getOrderCount();
    BigDecimal getRevenue();
}
