package com.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryForecastDto {
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private String baseUnit;
    private Integer availableStock;
    private Integer minStockAlert;
    private Integer totalSoldLast30Days;
    private Double avgDailySales;
    private Integer daysOfStockRemaining;
    private String riskLevel; // CRITICAL, WARNING, SAFE
    private String riskLevelLabel;
    private Integer suggestedRestockQty;
    private BigDecimal estimatedRestockCost;
}
