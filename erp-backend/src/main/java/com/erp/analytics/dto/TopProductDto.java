package com.erp.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDto {
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private long totalQuantitySold;
    private BigDecimal totalRevenue;
    private long currentStock;
}
