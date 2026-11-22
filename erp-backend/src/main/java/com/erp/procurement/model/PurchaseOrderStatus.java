package com.erp.procurement.model;

import lombok.Getter;

@Getter
public enum PurchaseOrderStatus {
    DRAFT("Dự thảo", "Đơn hàng đang soạn thảo, chưa gửi duyệt"),
    PENDING_APPROVAL("Chờ phê duyệt", "Đơn hàng đã gửi, đang chờ Giám đốc / Trưởng phòng duyệt"),
    APPROVED("Đã phê duyệt", "Đã duyệt, sẵn sàng gửi nhà cung cấp và nhập kho"),
    PARTIALLY_RECEIVED("Nhập kho một phần", "Đã nhận một phần số lượng hàng"),
    COMPLETED("Hoàn tất", "Đã nhập đủ 100% số lượng hàng vào kho"),
    CANCELLED("Đã hủy", "Đơn đặt hàng bị hủy");

    private final String label;
    private final String description;

    PurchaseOrderStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
