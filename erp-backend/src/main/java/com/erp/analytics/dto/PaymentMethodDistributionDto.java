package com.erp.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDistributionDto {
    private String method;           // CASH, BANK_TRANSFER, CREDIT_CARD, E_WALLET
    private String label;            // Tiền mặt, Chuyển khoản, Thẻ tín dụng, Ví điện tử
    private long count;              // Số lượng giao dịch
    private BigDecimal totalAmount;  // Tổng số tiền
    private double percentage;       // Tỷ trọng %
    private String color;            // Màu biểu đồ
}
