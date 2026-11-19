package com.erp.copilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO dữ liệu của Thẻ Đơn Hàng Dự Thảo (ORDER_DRAFT_CARD)
 * Hiển thị trực tiếp trong khung chat để người dùng kiểm duyệt và xác nhận 1-click
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftOrderDataDto {

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String paymentMethod; // CASH, BANK_TRANSFER, COD
    private String note;

    private List<DraftOrderItemDto> items;

    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private boolean allInStock;
    private String warningMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DraftOrderItemDto {
        private Long productId;
        private String sku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private Integer stockAvailable;
        private boolean isSufficient;
    }
}
