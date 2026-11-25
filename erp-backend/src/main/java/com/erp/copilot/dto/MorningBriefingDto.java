package com.erp.copilot.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MorningBriefingDto {

    private LocalDate briefingDate;
    private String greeting;
    private String executiveSummary;
    private String audioScriptText; // Kịch bản đọc giọng nói tiếng Việt

    // 1. Doanh thu & Lợi nhuận hôm qua
    private BigDecimal yesterdayRevenue;
    private BigDecimal yesterdayGrossProfit;
    private Integer yesterdayOrderCount;

    // 2. Công nợ đến hạn
    private BigDecimal dueDebtAmount;
    private Integer dueCustomerCount;

    // 3. PO chờ duyệt
    private Integer pendingPoCount;
    private BigDecimal pendingPoTotalAmount;

    // 4. Sản phẩm tiêu biểu
    private String topSellingProductName;
    private Integer topSellingQuantity;
    private String slowMovingProductName;
    private Integer slowMovingStockDays;

    private List<String> priorityActions;
}
