package com.erp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadBankStatementRequest {
    private String bankName; // Vietcombank, MBBank...
    private String accountNumber;
    private Long targetBankAccountId; // ID tài khoản quỹ ngân hàng để hạch toán tiền vào

    @NotBlank(message = "Nội dung dữ liệu sao kê không được để trống")
    private String statementRawContent; // Dữ liệu text dạng CSV hoặc các dòng sao kê copy-paste
}
