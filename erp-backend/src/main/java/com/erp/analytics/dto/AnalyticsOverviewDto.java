package com.erp.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewDto {
    // KPI Chính
    private BigDecimal totalRevenue;        // Tổng doanh thu hợp lệ (không tính đơn hủy)
    private BigDecimal completedRevenue;    // Doanh thu đơn đã hoàn tất
    private BigDecimal paidRevenue;         // Tiền đã thu thực tế
    private long totalOrders;               // Tổng số đơn
    private long completedOrders;           // Đơn hoàn thành
    private long cancelledOrders;           // Đơn hủy
    private long todayOrders;               // Đơn phát sinh hôm nay
    private BigDecimal averageOrderValue;   // Giá trị trung bình đơn
    private double completionRate;          // Tỷ lệ hoàn tất (%)

    // Kho & Tài sản
    private BigDecimal inventoryValuation;  // Tổng giá trị định giá kho (SUM(price * stock))
    private long totalStockUnits;           // Tổng số lượng cái/chiếc trong kho
    private long lowStockCount;             // Số mặt hàng sắp hết hàng (tồn <= 10)

    // Phân tích biểu đồ
    private List<TimeSeriesPointDto> revenueTrends;
    private List<OrderStatusDistributionDto> orderStatusBreakdown;
    private List<PaymentMethodDistributionDto> paymentMethodBreakdown;
    private List<TopProductDto> topSellingProducts;
    private List<LowStockAlertDto> lowStockAlerts;
}
