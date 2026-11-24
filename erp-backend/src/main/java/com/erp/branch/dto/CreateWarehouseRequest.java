package com.erp.branch.dto;

import com.erp.branch.model.WarehouseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateWarehouseRequest {
    @NotNull(message = "Chi nhánh trực thuộc không được để trống")
    private Long branchId;

    private String code; // Tùy chọn, nếu trống tự sinh

    @NotBlank(message = "Tên kho không được để trống")
    private String name;

    private WarehouseType warehouseType = WarehouseType.RETAIL_STORE;
    private String address;
    private Boolean active = true;
}
