package com.erp.inventory.model;

public enum SerialStatus {
    IN_STOCK,           // Đang tồn trong kho
    RESERVED,           // Giữ chỗ cho đơn hàng
    DELIVERED,          // Đã xuất kho giao khách
    ACTIVE_WARRANTY,    // Đang trong thời hạn bảo hành
    UNDER_REPAIR,       // Tiếp nhận sửa chữa/bảo hành
    RETURNED_DEFECTIVE  // Đổi trả hàng lỗi
}
