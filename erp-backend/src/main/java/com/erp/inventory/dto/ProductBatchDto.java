package com.erp.inventory.dto;

import com.erp.inventory.model.BatchStatus;
import com.erp.inventory.model.ProductBatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatchDto {
    private Long id;
    private String batchCode;
    private Long productId;
    private String productSku;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private Integer initialQuantity;
    private Integer remainingQuantity;
    private BigDecimal costPrice;
    private BatchStatus status;
    private String supplierName;
    private String notes;
    private Long daysUntilExpiry;
    private String riskZone; // GREEN, YELLOW, RED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductBatchDto fromEntity(ProductBatch b) {
        long days = 999;
        String zone = "GREEN";
        if (b.getExpiryDate() != null) {
            days = ChronoUnit.DAYS.between(LocalDate.now(), b.getExpiryDate());
            if (days <= 30) {
                zone = "RED";
            } else if (days <= 90) {
                zone = "YELLOW";
            } else {
                zone = "GREEN";
            }
        }

        return ProductBatchDto.builder()
                .id(b.getId())
                .batchCode(b.getBatchCode())
                .productId(b.getProductId())
                .productSku(b.getProductSku())
                .productName(b.getProductName())
                .warehouseId(b.getWarehouseId())
                .warehouseName(b.getWarehouseName())
                .manufacturingDate(b.getManufacturingDate())
                .expiryDate(b.getExpiryDate())
                .initialQuantity(b.getInitialQuantity())
                .remainingQuantity(b.getRemainingQuantity())
                .costPrice(b.getCostPrice())
                .status(b.getStatus())
                .supplierName(b.getSupplierName())
                .notes(b.getNotes())
                .daysUntilExpiry(days)
                .riskZone(zone)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
