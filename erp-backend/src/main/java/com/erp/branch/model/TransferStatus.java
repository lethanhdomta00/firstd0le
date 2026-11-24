package com.erp.branch.model;

public enum TransferStatus {
    DRAFT,          // Phiếu nháp / chờ duyệt
    IN_TRANSIT,     // Đã xuất kho, đang vận chuyển
    COMPLETED,      // Đã nhập kho đích hoàn tất
    CANCELLED       // Đã hủy điều chuyển
}
