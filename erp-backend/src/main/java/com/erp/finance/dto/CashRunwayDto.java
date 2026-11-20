package com.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashRunwayDto {
    private BigDecimal totalCashBalance; // Tổng tiền các quỹ (Tiền mặt + Ngân hàng)
    private BigDecimal totalCashOnHand; // Tiền mặt tại quỹ
    private BigDecimal totalBankBalance; // Tiền tại các tài khoản ngân hàng
    private BigDecimal monthlyBurnRate; // Tổng chi phí 30 ngày qua
    private BigDecimal dailyBurnRate; // Chi phí bình quân 1 ngày
    private int runwayDays; // Số ngày an toàn dòng tiền
    private String riskLevel; // SAFE (>= 90 ngày), WARNING (30-89 ngày), CRITICAL (< 30 ngày)
    private String riskTitle;
    private String recommendation; // Lời khuyên CFO điều hành
}
