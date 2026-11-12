package com.erp.order.model;

public enum InventoryMovementType {
    EXPORT_ORDER("Xuất kho đơn hàng"),
    IMPORT_CANCEL("Hoàn kho hủy/sửa đơn"),
    MANUAL_ADJUST("Điều chỉnh tồn kho");

    private final String label;

    InventoryMovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
