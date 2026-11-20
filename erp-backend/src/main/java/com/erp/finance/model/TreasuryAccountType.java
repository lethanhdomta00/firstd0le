package com.erp.finance.model;

public enum TreasuryAccountType {
    CASH("Tiền mặt tại quỹ"),
    BANK("Tài khoản ngân hàng");

    private final String title;

    TreasuryAccountType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
