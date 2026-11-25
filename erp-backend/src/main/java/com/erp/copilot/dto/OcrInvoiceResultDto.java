package com.erp.copilot.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrInvoiceResultDto {

    private String invoiceNumber;
    private String invoiceDate;
    private String supplierName;
    private String taxCode;
    private String supplierAddress;

    private List<OcrInvoiceItemDto> items;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private String suggestedDocumentType; // PURCHASE_ORDER hoặc PAYMENT_VOUCHER
    private Double confidenceScore; // Độ tin cậy (ví dụ 0.98 = 98%)

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OcrInvoiceItemDto {
        private String productName;
        private Integer quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
    }
}
