package com.erp.promotion.dto;

import com.erp.promotion.model.PricingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TieredPriceRuleDto {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private String tierName;
    private Integer minQuantity;
    private Integer maxQuantity;
    private PricingType pricingType;
    private BigDecimal discountPercent;
    private BigDecimal fixedPrice;
    private boolean active;
    private LocalDateTime createdAt;
}
