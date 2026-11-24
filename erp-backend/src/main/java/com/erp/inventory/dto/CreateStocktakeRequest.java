package com.erp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateStocktakeRequest {
    @NotNull(message = "Kho kiểm kê không được để trống")
    private Long warehouseId;

    @NotBlank(message = "Tiêu đề kiểm kê không được để trống")
    private String title;

    private String auditType = "PERIODIC"; // PERIODIC, SPOT_CHECK
    private String notes;

    // Danh sách sản phẩm kiểm kê (nếu để trống sẽ kiểm kê toàn bộ sản phẩm đang có tồn tại kho)
    private List<Long> productIds;
}
