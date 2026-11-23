package com.erp.promotion.model;

import com.erp.model.Product;
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
@Table(name = "tiered_price_rules", indexes = {
    @Index(name = "idx_tpr_product_id", columnList = "product_id"),
    @Index(name = "idx_tpr_active", columnList = "is_active")
})
public class TieredPriceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sản phẩm áp dụng bậc giá (Nếu null: áp dụng mặc định cho mọi sản phẩm)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @ToString.Exclude
    private Product product;

    @Column(name = "tier_name", nullable = false, length = 120)
    private String tierName;

    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 30)
    private PricingType pricingType = PricingType.PERCENT_DISCOUNT;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "fixed_price", precision = 18, scale = 2)
    private BigDecimal fixedPrice;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Kiểm tra số lượng mua có nằm trong khoảng bậc thang này không
     */
    public boolean matchesQuantity(int quantity) {
        if (!active) return false;
        if (minQuantity != null && quantity < minQuantity) return false;
        if (maxQuantity != null && quantity > maxQuantity) return false;
        return true;
    }
}
