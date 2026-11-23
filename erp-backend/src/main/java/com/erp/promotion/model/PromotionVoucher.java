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
@Table(name = "promotion_vouchers", indexes = {
    @Index(name = "idx_pv_code", columnList = "code"),
    @Index(name = "idx_pv_is_active", columnList = "is_active"),
    @Index(name = "idx_pv_dates", columnList = "start_date, end_date")
})
public class PromotionVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType = DiscountType.PERCENTAGE;

    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value", precision = 18, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "max_usage")
    private Integer maxUsage;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "max_usage_per_customer")
    private Integer maxUsagePerCustomer = 1;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.usedCount == null) this.usedCount = 0;
        if (this.minOrderValue == null) this.minOrderValue = BigDecimal.ZERO;
        if (this.code != null) this.code = this.code.trim().toUpperCase();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.code != null) this.code = this.code.trim().toUpperCase();
    }

    /**
     * Kiểm tra voucher có còn hạn sử dụng hợp lệ không
     */
    public boolean isValidDate(LocalDateTime now) {
        if (!active) return false;
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;
        return true;
    }

    /**
     * Kiểm tra số lượt sử dụng toàn hệ thống có bị vượt quá không
     */
    public boolean hasAvailableUsage() {
        if (maxUsage == null) return true;
        return (usedCount != null ? usedCount : 0) < maxUsage;
    }
}
