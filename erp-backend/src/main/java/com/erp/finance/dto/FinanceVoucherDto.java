package com.erp.finance.dto;

import com.erp.finance.model.CostCenter;
import com.erp.finance.model.FinanceVoucher;
import com.erp.finance.model.VoucherStatus;
import com.erp.finance.model.VoucherType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceVoucherDto {
    private Long id;
    private String voucherCode;
    private VoucherType voucherType;
    private String voucherTypeTitle;
    private CostCenter costCenter;
    private String costCenterTitle;
    private String categoryType;
    private BigDecimal amount;

    private Long sourceAccountId;
    private String sourceAccountName;
    private String sourceAccountCode;

    private Long targetAccountId;
    private String targetAccountName;
    private String targetAccountCode;

    private String payeePayorName;
    private String payeePayorPhone;
    private String referenceOrderCode;
    private String note;
    private VoucherStatus status;
    private String statusTitle;
    private String createdByUsername;
    private String approvedByUsername;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FinanceVoucherDto fromEntity(FinanceVoucher v) {
        if (v == null) return null;
        return FinanceVoucherDto.builder()
                .id(v.getId())
                .voucherCode(v.getVoucherCode())
                .voucherType(v.getVoucherType())
                .voucherTypeTitle(v.getVoucherType() != null ? v.getVoucherType().getTitle() : "")
                .costCenter(v.getCostCenter())
                .costCenterTitle(v.getCostCenter() != null ? v.getCostCenter().getTitle() : "")
                .categoryType(v.getCostCenter() != null ? v.getCostCenter().getCategoryType() : "")
                .amount(v.getAmount())
                .sourceAccountId(v.getSourceAccount() != null ? v.getSourceAccount().getId() : null)
                .sourceAccountName(v.getSourceAccount() != null ? v.getSourceAccount().getAccountName() : null)
                .sourceAccountCode(v.getSourceAccount() != null ? v.getSourceAccount().getAccountCode() : null)
                .targetAccountId(v.getTargetAccount() != null ? v.getTargetAccount().getId() : null)
                .targetAccountName(v.getTargetAccount() != null ? v.getTargetAccount().getAccountName() : null)
                .targetAccountCode(v.getTargetAccount() != null ? v.getTargetAccount().getAccountCode() : null)
                .payeePayorName(v.getPayeePayorName())
                .payeePayorPhone(v.getPayeePayorPhone())
                .referenceOrderCode(v.getReferenceOrderCode())
                .note(v.getNote())
                .status(v.getStatus())
                .statusTitle(v.getStatus() != null ? v.getStatus().getTitle() : "")
                .createdByUsername(v.getCreatedByUsername())
                .approvedByUsername(v.getApprovedByUsername())
                .approvedAt(v.getApprovedAt())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
