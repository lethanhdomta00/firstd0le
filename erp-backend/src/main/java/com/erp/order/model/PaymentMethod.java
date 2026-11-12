package com.erp.order.model;

public enum PaymentMethod {
    CASH("Tiền mặt / COD"),
    BANK_TRANSFER("Chuyển khoản ngân hàng"),
    CREDIT("Công nợ doanh nghiệp");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
