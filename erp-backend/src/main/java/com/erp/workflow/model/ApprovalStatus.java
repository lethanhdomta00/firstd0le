package com.erp.workflow.model;

import lombok.Getter;

@Getter
public enum ApprovalStatus {
    PENDING("Chờ phê duyệt", "Đang trong quá trình chờ các cấp xét duyệt"),
    APPROVED("Đã phê duyệt", "Đã được tất cả các cấp phê duyệt hoàn tất"),
    REJECTED("Đã từ chối", "Bị từ chối phê duyệt bởi một cấp có thẩm quyền"),
    CANCELLED("Đã hủy", "Người tạo hoặc hệ thống đã hủy bỏ yêu cầu phê duyệt");

    private final String label;
    private final String description;

    ApprovalStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
