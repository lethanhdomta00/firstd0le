package com.erp.finance.model;

public enum VoucherType {
    RECEIPT("Phiếu Thu", "Thu tiền vào quỹ"),
    PAYMENT("Phiếu Chi", "Chi tiền xuất quỹ"),
    INTERNAL_TRANSFER("Chuyển Quỹ Nội Bộ", "Luân chuyển tiền giữa các tài khoản");

    private final String title;
    private final String description;

    VoucherType(String title, String description) {
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
