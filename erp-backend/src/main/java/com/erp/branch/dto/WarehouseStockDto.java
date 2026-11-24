package com.erp.branch.dto;

import com.erp.branch.model.WarehouseStock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockDto {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long branchId;
    private Long productId;
    private String productSku;
    private String productName;
    private Integer physicalStock;
    private Integer availableStock;
    private Integer reservedStock;
    private Integer inTransitStock;
    private Integer minStockAlert;
    private Boolean isLowStock;
    private LocalDateTime updatedAt;

    public static WarehouseStockDto fromEntity(WarehouseStock ws) {
        int avail = ws.getAvailableStock() != null ? ws.getAvailableStock() : 0;
        int minAlert = ws.getMinStockAlert() != null ? ws.getMinStockAlert() : 10;
        return WarehouseStockDto.builder()
                .id(ws.getId())
                .warehouseId(ws.getWarehouseId())
                .warehouseName(ws.getWarehouseName())
                .branchId(ws.getBranchId())
                .productId(ws.getProductId())
                .productSku(ws.getProductSku())
                .productName(ws.getProductName())
                .physicalStock(ws.getPhysicalStock())
                .availableStock(avail)
                .reservedStock(ws.getReservedStock())
                .inTransitStock(ws.getInTransitStock())
                .minStockAlert(minAlert)
                .isLowStock(avail <= minAlert)
                .updatedAt(ws.getUpdatedAt())
                .build();
    }
}
