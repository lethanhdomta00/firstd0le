package com.erp.analytics.projection;

import java.math.BigDecimal;

public interface PaymentMethodProjection {
    String getPaymentMethod();
    Long getCount();
    BigDecimal getTotalAmount();
}
