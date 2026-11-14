package com.erp.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusDistributionDto {
    private String status;           // PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED
    private String label;            // Chờ xử lý, Đã xác nhận, Đang giao, Hoàn thành, Đã hủy
    private long count;              // Số lượng đơn
    private double percentage;       // Tỷ trọng %
    private String color;            // Màu sắc hex hiển thị trên biểu đồ
}
