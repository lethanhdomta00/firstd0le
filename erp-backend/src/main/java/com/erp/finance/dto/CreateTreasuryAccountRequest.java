package com.erp.finance.dto;

import com.erp.finance.model.TreasuryAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTreasuryAccountRequest {
    @NotBlank(message = "Mã tài khoản quỹ không được để trống")
    private String accountCode;

    @NotBlank(message = "Tên tài khoản quỹ không được để trống")
    private String accountName;

    @NotNull(message = "Loại tài khoản quỹ không được để trống (CASH hoặc BANK)")
    private TreasuryAccountType accountType;

    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private BigDecimal initialBalance;
    private String note;
}
