package com.erp.inventory.dto;

import com.erp.inventory.model.ProductSerial;
import com.erp.inventory.model.SerialStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSerialDto {
    private Long id;
    private String serialNumber;
    private Long productId;
    private String productSku;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private Long batchId;
    private SerialStatus status;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long orderId;
    private String orderCode;
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
    private Integer warrantyMonths;
    private Boolean isUnderWarranty;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductSerialDto fromEntity(ProductSerial s) {
        boolean underWarranty = false;
        if (s.getWarrantyEndDate() != null) {
            underWarranty = !LocalDate.now().isAfter(s.getWarrantyEndDate());
        }

        return ProductSerialDto.builder()
                .id(s.getId())
                .serialNumber(s.getSerialNumber())
                .productId(s.getProductId())
                .productSku(s.getProductSku())
                .productName(s.getProductName())
                .warehouseId(s.getWarehouseId())
                .warehouseName(s.getWarehouseName())
                .batchId(s.getBatchId())
                .status(s.getStatus())
                .customerId(s.getCustomerId())
                .customerName(s.getCustomerName())
                .customerPhone(s.getCustomerPhone())
                .orderId(s.getOrderId())
                .orderCode(s.getOrderCode())
                .warrantyStartDate(s.getWarrantyStartDate())
                .warrantyEndDate(s.getWarrantyEndDate())
                .warrantyMonths(s.getWarrantyMonths())
                .isUnderWarranty(underWarranty)
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
