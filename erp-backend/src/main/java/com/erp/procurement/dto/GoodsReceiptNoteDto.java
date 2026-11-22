package com.erp.procurement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptNoteDto {
    private Long id;
    private String grnCode;
    private Long purchaseOrderId;
    private String poCode;
    private Long supplierId;
    private String supplierName;
    private LocalDateTime receiptDate;
    private String warehouseLocation;
    private BigDecimal totalReceiptAmount;
    private String receivedByUsername;
    private String notes;
    private List<GoodsReceiptItemDto> items;
    private LocalDateTime createdAt;
}
