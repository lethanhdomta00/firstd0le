package com.erp.finance.model;

import lombok.Getter;

@Getter
public enum CostCenter {
    // NHÓM THU (INCOME)
    SALES_REVENUE("Doanh thu bán hàng", "INCOME"),
    DEBT_COLLECTION("Thu hồi nợ đại lý / khách hàng", "INCOME"),
    OTHER_INCOME("Thu nhập khác", "INCOME"),

    // NHÓM CHI VẬN HÀNH & KINH DOANH (EXPENSE / OPEX)
    RENTAL("Chi phí mặt bằng & kho bãi", "EXPENSE"),
    SALARY("Lương, thưởng & phụ cấp nhân sự", "EXPENSE"),
    MARKETING("Marketing & Quảng cáo truyền thông", "EXPENSE"),
    LOGISTICS("Vận chuyển & Bưu cục giao hàng", "EXPENSE"),
    PURCHASE("Nhập hàng & Thanh toán Nhà cung cấp", "EXPENSE"),
    UTILITIES("Điện, nước, internet & văn phòng phẩm", "EXPENSE"),
    ENTERTAINMENT("Tiếp khách, đối ngoại & công tác phí", "EXPENSE"),
    OTHER_EXPENSE("Chi phí hoạt động khác", "EXPENSE"),

    // LUÂN CHUYỂN NỘI BỘ
    INTERNAL_TRANSFER("Luân chuyển dòng tiền nội bộ", "TRANSFER");

    private final String title;
    private final String categoryType; // INCOME, EXPENSE, TRANSFER

    CostCenter(String title, String categoryType) {
        this.title = title;
        this.categoryType = categoryType;
    }
}
