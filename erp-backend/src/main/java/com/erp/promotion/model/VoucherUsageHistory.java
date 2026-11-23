package com.erp.promotion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Entity
@Table(name = "voucher_usage_histories", indexes = {
    @Index(name = "idx_vuh_voucher_code", columnList = "voucher_code"),
    @Index(name = "idx_vuh_customer_id", columnList = "customer_id"),
    @Index(name = "idx_vuh_order_id", columnList = "order_id")
})
public class VoucherUsageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_code", nullable = false, length = 50)
    private String voucherCode;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_code", nullable = false, length = 40)
    private String orderCode;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "order_subtotal", precision = 18, scale = 2)
    private BigDecimal orderSubtotal = BigDecimal.ZERO;

    @Column(name = "discount_applied", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountApplied = BigDecimal.ZERO;

    @Column(name = "used_at", updatable = false)
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        this.usedAt = LocalDateTime.now();
    }
}
