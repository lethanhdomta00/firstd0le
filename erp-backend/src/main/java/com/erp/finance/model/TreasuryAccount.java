package com.erp.finance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "treasury_accounts", indexes = {
        @Index(name = "idx_treasury_account_code", columnList = "accountCode", unique = true),
        @Index(name = "idx_treasury_account_type", columnList = "accountType"),
        @Index(name = "idx_treasury_account_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreasuryAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String accountCode; // CASH_OFFICE, BANK_VCB, BANK_MB, BANK_TCB

    @Column(nullable = false, length = 150)
    private String accountName; // "Quỹ tiền mặt văn phòng", "Vietcombank Công ty"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TreasuryAccountType accountType; // CASH, BANK

    @Column(length = 100)
    private String bankName; // Vietcombank, MBBank, Techcombank,...

    @Column(length = 50)
    private String accountNumber; // 0123456789

    @Column(length = 100)
    private String accountHolder; // CTY CP ERP CLOUD ENTERPRISE

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 255)
    private String note;

    @Version
    private Long version; // Optimistic Locking chống race condition nạp/rút tiền đồng thời

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
