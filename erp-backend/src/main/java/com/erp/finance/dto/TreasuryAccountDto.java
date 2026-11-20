package com.erp.finance.dto;

import com.erp.finance.model.TreasuryAccount;
import com.erp.finance.model.TreasuryAccountType;
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
public class TreasuryAccountDto {
    private Long id;
    private String accountCode;
    private String accountName;
    private TreasuryAccountType accountType;
    private String accountTypeTitle;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private BigDecimal currentBalance;
    private Boolean active;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TreasuryAccountDto fromEntity(TreasuryAccount account) {
        if (account == null) return null;
        return TreasuryAccountDto.builder()
                .id(account.getId())
                .accountCode(account.getAccountCode())
                .accountName(account.getAccountName())
                .accountType(account.getAccountType())
                .accountTypeTitle(account.getAccountType() != null ? account.getAccountType().getTitle() : "")
                .bankName(account.getBankName())
                .accountNumber(account.getAccountNumber())
                .accountHolder(account.getAccountHolder())
                .currentBalance(account.getCurrentBalance())
                .active(account.getActive())
                .note(account.getNote())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
