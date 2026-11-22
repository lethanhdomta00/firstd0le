package com.erp.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSupplierRequest {
    private String code;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    private String name;

    private String contactPerson;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    private String email;
    private String address;
    private String taxCode;
    private BigDecimal creditLimit;
    private Integer paymentTermsDays;
    private String status;
    private String notes;
}
