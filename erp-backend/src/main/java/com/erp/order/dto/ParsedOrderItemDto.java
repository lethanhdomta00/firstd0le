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
public class ParsedOrderItemDto {
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private Integer quantity;
    private String uomName;
    private Integer uomConversionRate;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private Integer availableStock;
    private boolean inStock;
}
