package com.erp.finance.model;

public enum ReconciliationStatus {
    PENDING("Chờ đối soát", "Dòng sao kê mới nạp"),
    MATCHED("Khớp đơn hàng", "Đã tìm thấy mã đơn hàng phù hợp"),
    RECONCILED("Đã đối soát & hạch toán", "Đã cập nhật đơn PAID và tăng số dư quỹ"),
    UNMATCHED("Không khớp", "Không tìm thấy mã đơn hoặc số tiền lệch");

    private final String title;
    private final String description;

    ReconciliationStatus(String title, String description) {
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
