package com.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO Báo cáo Tuổi nợ Đại lý / Khách hàng (Aging Debt Report)
 * Phân tích công nợ theo các khung thời gian: 0-30 ngày, 31-60 ngày, 61-90 ngày, nợ xấu > 90 ngày.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDebtAgingDto {
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String priceTier;
    private BigDecimal creditLimit;
    private BigDecimal currentDebt;

    // Phân loại tuổi nợ theo ngày
    private BigDecimal currentDue;      // 0 - 30 ngày (Trong hạn thanh toán)
    private BigDecimal overdue31To60;   // 31 - 60 ngày (Quá hạn nhẹ)
    private BigDecimal overdue61To90;   // 61 - 90 ngày (Cảnh báo đỏ)
    private BigDecimal badDebtOver90;   // > 90 ngày (Nợ xấu khó đòi)

    private Integer maxOverdueDays;     // Số ngày quá hạn lâu nhất của khách
    private Boolean isCreditLocked;     // Bị khóa nợ nếu quá hạn > 60 ngày hoặc vượt hạn mức
}
