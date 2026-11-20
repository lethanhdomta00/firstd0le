package com.erp.finance.dto;

import com.erp.finance.model.CostCenter;
import com.erp.finance.model.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFinanceVoucherRequest {
    @NotNull(message = "Loại phiếu không được để trống (RECEIPT hoặc PAYMENT)")
    private VoucherType voucherType;

    @NotNull(message = "Trung tâm chi phí / Nguồn thu không được để trống")
    private CostCenter costCenter;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "1000", message = "Số tiền phải tối thiểu 1.000 VNĐ")
    private BigDecimal amount;

    private Long sourceAccountId; // Bắt buộc nếu là PAYMENT
    private Long targetAccountId; // Bắt buộc nếu là RECEIPT

    private String payeePayorName;
    private String payeePayorPhone;
    private String referenceOrderCode;
    private String note;

    private boolean autoApprove = false; // Nếu người tạo có thẩm quyền duyệt ngay
}
