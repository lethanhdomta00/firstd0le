package com.erp.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ValidateVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    private Long customerId;

    private String customerPhone;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    private BigDecimal orderSubtotal;
}
