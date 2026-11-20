package com.erp.finance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_vouchers", indexes = {
        @Index(name = "idx_finance_voucher_code", columnList = "voucherCode", unique = true),
        @Index(name = "idx_finance_voucher_type", columnList = "voucherType"),
        @Index(name = "idx_finance_voucher_status", columnList = "status"),
        @Index(name = "idx_finance_voucher_cost_center", columnList = "costCenter"),
        @Index(name = "idx_finance_voucher_ref_order", columnList = "referenceOrderCode"),
        @Index(name = "idx_finance_voucher_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String voucherCode; // RC-20261120-001, PV-20261120-001, TF-20261120-001

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VoucherType voucherType; // RECEIPT, PAYMENT, INTERNAL_TRANSFER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CostCenter costCenter; // RENTAL, SALARY, SALES_REVENUE,...

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    // Tài khoản nguồn (dùng cho Phiếu Chi và Chuyển Quỹ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private TreasuryAccount sourceAccount;

    // Tài khoản đích (dùng cho Phiếu Thu và Chuyển Quỹ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private TreasuryAccount targetAccount;

    @Column(length = 150)
    private String payeePayorName; // Người nộp tiền hoặc người nhận tiền

    @Column(length = 30)
    private String payeePayorPhone;

    @Column(length = 50)
    private String referenceOrderCode; // Mã đơn hàng ORD-... nếu là thu tiền đơn

    @Column(length = 500)
    private String note; // Lý do thu/chi/chuyển tiền

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private VoucherStatus status = VoucherStatus.DRAFT;

    @Column(length = 100)
    private String createdByUsername;

    @Column(length = 100)
    private String approvedByUsername;

    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
