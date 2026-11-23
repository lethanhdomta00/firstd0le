package com.erp.promotion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CalculateCartDiscountsResponse {

    private BigDecimal originalSubtotal;       // Tổng tiền giá gốc trước chiết khấu
    private BigDecimal tieredDiscountTotal;    // Tổng tiền giảm từ giá sỉ bậc thang
    private BigDecimal subtotalAfterTiered;    // Tạm tính sau khi trừ giá sỉ bậc thang
    private BigDecimal voucherDiscount;        // Giảm từ Voucher
    private String appliedVoucherCode;
    private String voucherMessage;
    private Integer loyaltyPointsRedeemed;     // Điểm tích lũy đã dùng
    private BigDecimal loyaltyDiscount;        // Giảm từ điểm tích lũy (1 điểm = 1.000đ)
    private BigDecimal finalTotal;             // Tổng tiền cần thanh toán
    private Integer estimatedPointsEarned;     // Điểm dự kiến sẽ tích lũy khi hoàn thành đơn
    private List<CalculatedItemResult> items;

    @Data
    @Builder
    public static class CalculatedItemResult {
        private Long productId;
        private Integer quantity;
        private BigDecimal baseUnitPrice;
        private BigDecimal effectiveUnitPrice; // Đơn giá sau giá sỉ bậc thang
        private BigDecimal lineTotal;
        private BigDecimal lineDiscount;
        private String appliedTierName;
    }
}
