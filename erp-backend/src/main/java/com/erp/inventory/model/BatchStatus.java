package com.erp.inventory.model;

public enum BatchStatus {
    ACTIVE,         // Đang hoạt động, còn hạn và còn tồn
    NEAR_EXPIRY,    // Cận date (dưới 30 ngày)
    EXPIRED,        // Đã hết hạn sử dụng
    DEPLETED        // Đã xuất hết tồn kho (remaining = 0)
}
