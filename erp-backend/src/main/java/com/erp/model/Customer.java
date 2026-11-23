package com.erp.model;

import com.erp.promotion.model.MemberTier;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_phone", columnList = "phone"),
    @Index(name = "idx_customers_name", columnList = "name"),
    @Index(name = "idx_customers_member_tier", columnList = "member_tier")
})
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(unique = true, nullable = false, length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 500)
    private String address;

    /**
     * Bậc giá áp dụng: RETAIL (Lẻ), DROPSHIP, WHOLESALE_L1 (Sỉ cấp 1), VIP
     */
    @Column(name = "price_tier", length = 32)
    private String priceTier = "RETAIL";

    /**
     * Hạn mức công nợ tối đa cho phép (Credit Limit)
     * Mặc định 50.000.000 VNĐ cho khách hàng doanh nghiệp / đại lý
     */
    @Column(name = "credit_limit", precision = 18, scale = 2)
    private BigDecimal creditLimit = new BigDecimal("50000000.00");

    /**
     * Dư nợ hiện tại đang nợ hệ thống (Current Debt)
     */
    @Column(name = "current_debt", precision = 18, scale = 2)
    private BigDecimal currentDebt = BigDecimal.ZERO;

    @Column(name = "overdue_days")
    private Integer overdueDays = 0;

    /**
     * Khóa công nợ (Nếu true: không cho phép tạo đơn nợ, bắt buộc trả trước)
     */
    @Column(name = "is_credit_locked")
    private boolean creditLocked = false;

    /**
     * Điểm thưởng tích lũy hội viên (Loyalty Points)
     * Quy ước: 1 điểm = 1.000 VNĐ khi khấu trừ vào đơn hàng
     */
    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    /**
     * Tổng chi tiêu trọn đời của khách hàng (Lifetime Total Spent)
     * Dùng để tự động xét thăng hạng thành viên
     */
    @Column(name = "total_spent", precision = 18, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    /**
     * Hạng hội viên: BRONZE (Đồng), SILVER (Bạc), GOLD (Vàng), DIAMOND (Kim Cương)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "member_tier", nullable = false, length = 30)
    private MemberTier memberTier = MemberTier.BRONZE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.creditLimit == null) this.creditLimit = new BigDecimal("50000000.00");
        if (this.currentDebt == null) this.currentDebt = BigDecimal.ZERO;
        if (this.priceTier == null) this.priceTier = "RETAIL";
        if (this.loyaltyPoints == null) this.loyaltyPoints = 0;
        if (this.totalSpent == null) this.totalSpent = BigDecimal.ZERO;
        if (this.memberTier == null) this.memberTier = MemberTier.BRONZE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.memberTier == null) this.memberTier = MemberTier.BRONZE;
    }

    /**
     * Kiểm tra xem nếu mua thêm đơn hàng với số tiền này có bị vượt hạn mức công nợ không
     */
    public boolean isExceedingCreditLimit(BigDecimal additionalAmount) {
        if (additionalAmount == null || additionalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal debt = (this.currentDebt != null) ? this.currentDebt : BigDecimal.ZERO;
        BigDecimal limit = (this.creditLimit != null) ? this.creditLimit : BigDecimal.ZERO;
        return debt.add(additionalAmount).compareTo(limit) > 0;
    }

    /**
     * Cộng thêm chi tiêu và tự động cập nhật hạng hội viên
     */
    public void addSpentAndEvaluateTier(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.totalSpent = (this.totalSpent != null ? this.totalSpent : BigDecimal.ZERO).add(amount);
            this.memberTier = MemberTier.determineTier(this.totalSpent);
        }
    }
}
