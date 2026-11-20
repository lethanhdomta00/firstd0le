package com.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitAndLossDto {
    private String periodTitle; // "Tháng 11/2026", "Hôm nay", "Năm 2026"
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // 1. DOANH THU (REVENUE)
    @Builder.Default
    private BigDecimal grossRevenue = BigDecimal.ZERO; // Tổng doanh số

    @Builder.Default
    private BigDecimal discounts = BigDecimal.ZERO; // Chiết khấu & Giảm giá

    @Builder.Default
    private BigDecimal netRevenue = BigDecimal.ZERO; // Doanh thu thuần = Gross - Discounts

    // 2. GIÁ VỐN HÀNG BÁN (COGS)
    @Builder.Default
    private BigDecimal cogs = BigDecimal.ZERO; // Cost of Goods Sold

    // 3. LỢI NHUẬN GỘP (GROSS PROFIT)
    @Builder.Default
    private BigDecimal grossProfit = BigDecimal.ZERO; // Net Revenue - COGS

    private double grossMarginPercent; // Gross Profit / Net Revenue * 100

    // 4. CHI PHÍ HOẠT ĐỘNG (OPEX)
    @Builder.Default
    private BigDecimal totalOpex = BigDecimal.ZERO;

    @Builder.Default
    private List<CostCenterExpenseItem> opexBreakdown = new ArrayList<>();

    // 5. LỢI NHUẬN RÒNG / THUẦN (EBIT / NET PROFIT)
    @Builder.Default
    private BigDecimal netProfit = BigDecimal.ZERO; // Gross Profit - OPEX

    private double netMarginPercent; // Net Profit / Net Revenue * 100

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostCenterExpenseItem {
        private String costCenterCode;
        private String costCenterTitle;
        private BigDecimal amount;
        private double percentageOfOpex;
        private long voucherCount;
    }
}
