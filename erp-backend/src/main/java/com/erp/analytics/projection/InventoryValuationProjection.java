package com.erp.analytics.projection;

import java.math.BigDecimal;

public interface InventoryValuationProjection {
    BigDecimal getTotalValuation();
    Long getTotalUnits();
    Long getLowStockCount();
}
