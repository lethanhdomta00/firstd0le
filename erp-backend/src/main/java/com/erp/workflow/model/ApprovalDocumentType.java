package com.erp.workflow.model;

import lombok.Getter;

@Getter
public enum ApprovalDocumentType {
    PURCHASE_ORDER("Đơn mua hàng PO", "Purchase Order từ nhà cung cấp"),
    PAYMENT_VOUCHER("Phiếu chi Quỹ", "Phiếu chi xuất quỹ tài chính treasury"),
    SPECIAL_DISCOUNT("Chiết khấu đặc biệt", "Chiết khấu vượt hạn mức nhân viên tại POS");

    private final String label;
    private final String description;

    ApprovalDocumentType(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
