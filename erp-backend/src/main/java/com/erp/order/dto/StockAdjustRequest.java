package com.erp.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustRequest {
    @NotNull(message = "Vui lòng chọn sản phẩm cần điều chỉnh kho")
    private Long productId;

    @NotNull(message = "Số lượng điều chỉnh không được để trống")
    private Integer quantityDelta;

    private String note;
}
