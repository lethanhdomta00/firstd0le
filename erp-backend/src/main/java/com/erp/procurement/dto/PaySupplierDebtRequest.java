package com.erp.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaySupplierDebtRequest {
    @NotNull(message = "Nhà cung cấp không được để trống")
    private Long supplierId;

    private Long purchaseOrderId; // Tùy chọn nếu thanh toán cụ thể cho 1 đơn PO

    @NotNull(message = "Số tiền thanh toán không được để trống")
    private BigDecimal amount;

    @NotBlank(message = "Tài khoản nguồn thanh toán không được để trống")
    private String sourceAccountCode; // Vd: BANK_VCB, CASH_OFFICE

    private String notes;
}
