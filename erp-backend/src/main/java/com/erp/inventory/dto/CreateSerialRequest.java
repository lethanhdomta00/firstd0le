package com.erp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSerialRequest {
    @NotBlank(message = "Mã Serial / IMEI không được để trống")
    private String serialNumber;

    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    private Long warehouseId;
    private Long batchId;
    private Integer warrantyMonths = 12;
    private String notes;
}
