package com.erp.workflow.dto;

import com.erp.workflow.model.ApprovalDocumentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitApprovalRequest {

    @NotNull(message = "Loại chứng từ không được để trống")
    private ApprovalDocumentType documentType;

    @NotNull(message = "ID chứng từ không được để trống")
    private Long documentId;

    @NotBlank(message = "Mã chứng từ không được để trống")
    private String documentCode;

    @NotBlank(message = "Tiêu đề yêu cầu phê duyệt không được để trống")
    private String title;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Số tiền phải >= 0")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "VND";

    private String payloadSnapshot;
}
