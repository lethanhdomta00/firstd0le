package com.erp.promotion.dto;

import com.erp.promotion.model.MemberTier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoyaltyMemberDto {
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private MemberTier memberTier;
    private String tierDisplayName;
    private double pointMultiplier;
    private Integer loyaltyPoints;
    private BigDecimal totalSpent;
    private BigDecimal pointsValueVnd; // Giá trị quy đổi tiền mặt (1 điểm = 1.000đ)
}
