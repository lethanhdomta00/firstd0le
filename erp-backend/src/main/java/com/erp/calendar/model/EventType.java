package com.erp.calendar.model;

public enum EventType {
    MEETING,            // Cuộc họp nội bộ / Họp dự án
    CLIENT_MEETING,     // Gặp đối tác / Khách hàng
    DEADLINE,           // Hạn chót công việc / Bàn giao
    TASK,               // Nhiệm vụ / Công việc cần làm
    TRAINING,           // Đào tạo / Workshop / Tập huấn
    INTERVIEW,          // Phỏng vấn / Tuyển dụng ứng viên
    BUSINESS_TRIP,      // Đi công tác / Khảo sát thị trường
    REVIEW,             // Đánh giá hiệu suất / Review KPI
    TEAM_BUILDING,      // Team Building / Hoạt động nội bộ
    MAINTENANCE,        // Bảo trì hệ thống / Hạ tầng
    HOLIDAY,            // Nghỉ lễ / Nghỉ phép công ty
    EVENT               // Sự kiện chung khác
}
