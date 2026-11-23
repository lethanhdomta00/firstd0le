package com.erp.promotion.model;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public enum MemberTier {
    BRONZE("Đồng", 1.0, BigDecimal.ZERO),
    SILVER("Bạc", 1.2, new BigDecimal("5000000")),
    GOLD("Vàng", 1.5, new BigDecimal("20000000")),
    DIAMOND("Kim Cương", 2.0, new BigDecimal("50000000"));

    private final String displayName;
    private final double pointMultiplier;
    private final BigDecimal minSpend;

    MemberTier(String displayName, double pointMultiplier, BigDecimal minSpend) {
        this.displayName = displayName;
        this.pointMultiplier = pointMultiplier;
        this.minSpend = minSpend;
    }

    /**
     * Xác định hạng hội viên dựa trên tổng chi tiêu trọn đời
     */
    public static MemberTier determineTier(BigDecimal totalSpent) {
        if (totalSpent == null || totalSpent.compareTo(BigDecimal.ZERO) <= 0) {
            return BRONZE;
        }
        if (totalSpent.compareTo(DIAMOND.minSpend) >= 0) {
            return DIAMOND;
        }
        if (totalSpent.compareTo(GOLD.minSpend) >= 0) {
            return GOLD;
        }
        if (totalSpent.compareTo(SILVER.minSpend) >= 0) {
            return SILVER;
        }
        return BRONZE;
    }
}
