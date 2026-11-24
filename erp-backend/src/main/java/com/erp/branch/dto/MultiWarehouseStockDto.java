package com.erp.branch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiWarehouseStockDto {
    private Long productId;
    private String productSku;
    private String productName;
    private String baseUnit;
    private BigDecimal price;
    private Integer totalPhysicalStock;
    private Integer totalAvailableStock;
    private Integer totalReservedStock;
    private Integer totalInTransitStock;
    @Builder.Default
    private List<WarehouseStockBreakdownDto> breakdown = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseStockBreakdownDto {
        private Long warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String warehouseType;
        private Long branchId;
        private String branchName;
        private Integer physicalStock;
        private Integer availableStock;
        private Integer reservedStock;
        private Integer inTransitStock;
        private Integer minStockAlert;
        private Boolean isLowStock;
    }
}
