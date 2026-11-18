package com.erp.order.dto;

import com.erp.order.model.PaymentMethod;
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
public class ParsedOrderResultDto {
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private PaymentMethod paymentMethod;
    private String paymentMethodLabel;
    private String note;
    private List<ParsedOrderItemDto> items;
    private BigDecimal estimatedTotal;
    private Double confidenceScore;
    private String summary;
    private List<String> warnings;
}
