package com.erp.promotion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PromotionOverviewDto {
    private long totalVouchers;
    private long activeVouchers;
    private BigDecimal totalDiscountSponsored; // Tổng số tiền chiết khấu hệ thống đã tài trợ
    private long totalLoyaltyMembers;
    private long totalPointsOutstanding;       // Tổng điểm tích lũy của khách hàng chưa sử dụng
    private long totalTieredRules;
}
