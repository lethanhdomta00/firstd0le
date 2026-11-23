package com.erp.promotion.model;

public enum LoyaltyTransactionType {
    EARN,    // Tích điểm từ đơn hàng hoàn thành
    REDEEM,  // Sử dụng điểm trừ tiền đơn hàng
    REFUND,  // Hoàn trả điểm do đơn hàng hủy
    ADJUST   // Điều chỉnh cộng/trừ thủ công bởi quản trị viên
}
