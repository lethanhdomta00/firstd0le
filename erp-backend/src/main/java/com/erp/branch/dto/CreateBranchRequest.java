package com.erp.branch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBranchRequest {
    private String code; // Tùy chọn, nếu trống tự sinh theo BR-XXX

    @NotBlank(message = "Tên chi nhánh không được để trống")
    private String name;

    private String address;
    private String phone;
    private String status = "ACTIVE";
    private Boolean isHeadquarter = false;
}
