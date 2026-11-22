package com.erp.order.model;

public enum InventoryMovementType {
    EXPORT_ORDER("Xuất kho đơn hàng"),
    IMPORT_CANCEL("Hoàn kho hủy/sửa đơn"),
    IMPORT_PURCHASE("Nhập kho mua hàng từ NCC"),
    MANUAL_ADJUST("Điều chỉnh tồn kho");

    private final String label;

    InventoryMovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
