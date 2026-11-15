package com.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VietQrPaymentDto {
    private Long orderId;
    private String orderCode;
    private String customerName;
    private BigDecimal totalAmount;
    private String bankId;
    private String bankName;
    private String accountNo;
    private String accountName;
    private String transferContent;
    private String qrImageUrl;
    private boolean paid;
}
