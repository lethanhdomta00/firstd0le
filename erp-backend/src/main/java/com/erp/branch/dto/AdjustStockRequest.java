package com.erp.branch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdjustStockRequest {
    @NotNull(message = "Kho không được để trống")
    private Long warehouseId;

    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Số lượng tồn thực tế mới không được để trống")
    private Integer newPhysicalStock;

    private Integer minStockAlert;
    private String reason;
}
