package com.erp.analytics.projection;

import java.math.BigDecimal;

public interface TopProductProjection {
    Long getProductId();
    String getProductName();
    String getProductSku();
    String getProductImageUrl();
    Long getTotalQuantity();
    BigDecimal getTotalRevenue();
    Long getCurrentStock();
}
