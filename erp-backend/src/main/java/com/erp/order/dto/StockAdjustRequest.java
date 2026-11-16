package com.erp.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockAdjustRequest {
    @NotNull(message = "Vui lòng chọn sản phẩm cần điều chỉnh kho")
    private Long productId;

    @NotNull(message = "Số lượng điều chỉnh không được để trống")
    private Integer quantityDelta;

    /**
     * Giá nhập lô hàng mới (dùng để tự động tính lại Giá vốn bình quân Moving Average Cost khi nhập kho)
     */
    private BigDecimal inboundCostPrice;

    private String note;
}
