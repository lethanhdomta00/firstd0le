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
public class PaymentWebhookRequest {
    private String gateway;
    private String transactionId;
    private String content;
    private BigDecimal amount;
    private String orderCode;
}
