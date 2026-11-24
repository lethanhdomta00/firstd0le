package com.erp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAllocationDto {
    private Long productId;
    private String productSku;
    private String productName;
    private Integer requestedQuantity;
    private Integer allocatedQuantity;
    private String strategy; // FEFO, FIFO
    @Builder.Default
    private List<AllocatedBatchItemDto> allocations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocatedBatchItemDto {
        private Long batchId;
        private String batchCode;
        private Long warehouseId;
        private String warehouseName;
        private LocalDate expiryDate;
        private Integer remainingQuantity;
        private Integer quantityToPick;
    }
}
