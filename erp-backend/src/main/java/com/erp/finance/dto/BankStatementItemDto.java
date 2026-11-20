package com.erp.finance.dto;

import com.erp.finance.model.BankStatementItem;
import com.erp.finance.model.ReconciliationStatus;
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
public class BankStatementItemDto {
    private Long id;
    private String transactionCode;
    private LocalDateTime transactionDate;
    private BigDecimal amount;
    private String transactionType; // CR, DR
    private String description;
    private String bankName;
    private String accountNumber;
    private String matchedOrderCode;
    private Long matchedOrderId;
    private String matchedCustomerName;
    private ReconciliationStatus status;
    private String statusTitle;
    private String reconciliationNote;
    private Long createdVoucherId;
    private LocalDateTime createdAt;

    public static BankStatementItemDto fromEntity(BankStatementItem item) {
        if (item == null) return null;
        return BankStatementItemDto.builder()
                .id(item.getId())
                .transactionCode(item.getTransactionCode())
                .transactionDate(item.getTransactionDate())
                .amount(item.getAmount())
                .transactionType(item.getTransactionType())
                .description(item.getDescription())
                .bankName(item.getBankName())
                .accountNumber(item.getAccountNumber())
                .matchedOrderCode(item.getMatchedOrderCode())
                .matchedOrderId(item.getMatchedOrderId())
                .matchedCustomerName(item.getMatchedCustomerName())
                .status(item.getStatus())
                .statusTitle(item.getStatus() != null ? item.getStatus().getTitle() : "")
                .reconciliationNote(item.getReconciliationNote())
                .createdVoucherId(item.getCreatedVoucherId())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
