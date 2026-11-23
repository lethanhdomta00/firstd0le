package com.erp.promotion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CalculateCartDiscountsRequest {

    private Long customerId;
    private String customerPhone;
    private String voucherCode;
    private Integer loyaltyPointsToRedeem;
    private List<CartItemInput> items;

    @Data
    public static class CartItemInput {
        private Long productId;
        private Integer quantity;
        private BigDecimal baseUnitPrice; // Giá bán cơ sở
    }
}
