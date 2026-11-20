package com.erp.finance.model;

public enum VoucherStatus {
    DRAFT("Dự thảo", "Chưa hạch toán vào số dư quỹ"),
    APPROVED("Đã duyệt", "Đã hạch toán cộng/trừ số dư quỹ thành công"),
    CANCELLED("Đã hủy", "Hủy bỏ phiếu, không tác động số dư");

    private final String title;
    private final String description;

    VoucherStatus(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
