package com.erp.workflow.dto;

import com.erp.workflow.model.ApprovalDocumentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalMatrixRuleDto {

    private Long id;

    @NotBlank(message = "Tên quy tắc không được để trống")
    private String ruleName;

    @NotNull(message = "Loại chứng từ không được để trống")
    private ApprovalDocumentType documentType;

    @NotNull(message = "Hạn mức tối thiểu không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Hạn mức tối thiểu phải >= 0")
    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    @NotNull(message = "Cấp độ duyệt không được để trống")
    private Integer level;

    @NotBlank(message = "Vai trò người duyệt không được để trống")
    private String approverRole;

    @Builder.Default
    private Boolean autoApprove = false;

    @Builder.Default
    private Integer priority = 1;

    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;
}
