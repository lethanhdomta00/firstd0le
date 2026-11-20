package com.erp.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InternalTransferRequest {
    @NotNull(message = "Tài khoản trích tiền (nguồn) không được để trống")
    private Long sourceAccountId;

    @NotNull(message = "Tài khoản nhận tiền (đích) không được để trống")
    private Long targetAccountId;

    @NotNull(message = "Số tiền chuyển không được để trống")
    @DecimalMin(value = "1000", message = "Số tiền chuyển tối thiểu 1.000 VNĐ")
    private BigDecimal amount;

    private String note;
}
