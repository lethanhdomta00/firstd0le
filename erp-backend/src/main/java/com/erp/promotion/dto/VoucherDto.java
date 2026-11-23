package com.erp.promotion.dto;

import com.erp.promotion.model.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VoucherDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxUsage;
    private Integer usedCount;
    private Integer maxUsagePerCustomer;
    private boolean active;
    private LocalDateTime createdAt;
    private boolean isExpired;
}
