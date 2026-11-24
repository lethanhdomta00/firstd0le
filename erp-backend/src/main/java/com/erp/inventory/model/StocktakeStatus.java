package com.erp.inventory.model;

public enum StocktakeStatus {
    DRAFT,          // Mới tạo phiếu, chưa đếm
    IN_PROGRESS,    // Đang tiến hành quét / đếm thực tế
    BALANCED,       // Đã cân đối & điều chỉnh kho hoàn tất
    CANCELLED       // Đã hủy phiếu kiểm kê
}
