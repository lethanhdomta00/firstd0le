package com.erp.model;

import lombok.Getter;
import java.util.Set;

@Getter
public enum Role {
    ADMIN("Quản trị viên tối cao", "Toàn quyền quản trị hệ thống, quản lý tài khoản và cấu hình",
            Set.of("USER_MANAGE", "ORDER_ALL", "PRODUCT_ALL", "ANALYTICS_ALL", "INVENTORY_ALL")),

    MANAGER("Giám đốc / Quản lý", "Điều hành vận hành, duyệt đơn hàng, quản lý sản phẩm và xem báo cáo BI",
            Set.of("USER_READ", "ORDER_ALL", "PRODUCT_ALL", "ANALYTICS_ALL", "INVENTORY_READ")),

    ACCOUNTANT("Kế toán tài chính", "Kiểm soát dòng tiền, xuất hóa đơn tài chính, xem đơn hàng và phân tích doanh thu",
            Set.of("ORDER_READ", "ORDER_EXPORT", "ANALYTICS_ALL")),

    WAREHOUSE("Thủ kho", "Quản lý tồn kho, nhập/xuất kho, cập nhật số lượng và điều chỉnh sản phẩm",
            Set.of("PRODUCT_ALL", "INVENTORY_ALL", "ORDER_READ")),

    SALES("Nhân viên kinh doanh", "Tạo và xử lý đơn hàng, theo dõi khách hàng và xem thông tin sản phẩm",
            Set.of("ORDER_READ", "ORDER_CREATE", "ORDER_EDIT", "PRODUCT_READ")),

    USER("Nhân viên cơ bản", "Xem thông tin cơ bản và sử dụng các tiện ích nội bộ",
            Set.of("PRODUCT_READ", "ORDER_READ"));

    private final String title;
    private final String description;
    private final Set<String> permissions;

    Role(String title, String description, Set<String> permissions) {
        this.title = title;
        this.description = description;
        this.permissions = permissions;
    }
}
