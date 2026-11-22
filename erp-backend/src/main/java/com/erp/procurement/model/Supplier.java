package com.erp.procurement.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_suppliers_code", columnList = "code", unique = true),
        @Index(name = "idx_suppliers_phone", columnList = "phone"),
        @Index(name = "idx_suppliers_status", columnList = "status"),
        @Index(name = "idx_suppliers_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // NCC-001, XUONG-MAY-01

    @Column(nullable = false, length = 150)
    private String name; // Tên nhà cung cấp / Xưởng may

    @Column(length = 100)
    private String contactPerson; // Người đại diện / Chuyên viên bán sỉ

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 50)
    private String taxCode; // Mã số thuế

    /**
     * Hạn mức công nợ tối đa cho phép nợ NCC (Credit Limit)
     * Ví dụ: 200.000.000 VNĐ
     */
    @Column(name = "credit_limit", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal creditLimit = new BigDecimal("200000000.00");

    /**
     * Tổng công nợ hiện tại doanh nghiệp đang nợ NCC (Current Debt - Accounts Payable)
     */
    @Column(name = "current_debt", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal currentDebt = BigDecimal.ZERO;

    /**
     * Điều khoản thời hạn thanh toán nợ (số ngày cho phép, vd: 30 ngày)
     */
    @Column(name = "payment_terms_days")
    @Builder.Default
    private Integer paymentTermsDays = 30;

    /**
     * Trạng thái hoạt động: ACTIVE, INACTIVE
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(length = 500)
    private String notes;

    @Version
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
