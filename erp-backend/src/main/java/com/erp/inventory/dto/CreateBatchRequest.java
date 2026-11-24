package com.erp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateBatchRequest {
    private String batchCode; // Nếu trống sẽ tự sinh LOT-yyyyMM-XXXX

    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Kho hàng không được để trống")
    private Long warehouseId;

    private LocalDate manufacturingDate;

    @NotNull(message = "Hạn sử dụng không được để trống")
    private LocalDate expiryDate;

    @NotNull(message = "Số lượng nhập ban đầu phải lớn hơn 0")
    private Integer initialQuantity;

    private BigDecimal costPrice;
    private String supplierName;
    private String notes;
}
