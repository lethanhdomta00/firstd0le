package com.erp.procurement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptItemDto {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Integer quantityReceived;
    private BigDecimal importUnitPrice;
    private BigDecimal lineTotal;
    private BigDecimal oldCostPrice;
    private BigDecimal newCostPrice;
    private Integer oldPhysicalStock;
    private Integer newPhysicalStock;
}
