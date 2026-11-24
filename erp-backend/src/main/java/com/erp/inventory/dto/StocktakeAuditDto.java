package com.erp.inventory.dto;

import com.erp.inventory.model.StocktakeAudit;
import com.erp.inventory.model.StocktakeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StocktakeAuditDto {
    private Long id;
    private String auditCode;
    private String title;
    private Long warehouseId;
    private String warehouseName;
    private String auditType;
    private StocktakeStatus status;
    private Integer totalItems;
    private Integer totalDiscrepancyQty;
    private String createdBy;
    private String approvedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private List<StocktakeItemDto> items = new ArrayList<>();

    public static StocktakeAuditDto fromEntity(StocktakeAudit a) {
        List<StocktakeItemDto> itemDtos = new ArrayList<>();
        if (a.getItems() != null) {
            itemDtos = a.getItems().stream()
                    .map(StocktakeItemDto::fromEntity)
                    .toList();
        }
        return StocktakeAuditDto.builder()
                .id(a.getId())
                .auditCode(a.getAuditCode())
                .title(a.getTitle())
                .warehouseId(a.getWarehouseId())
                .warehouseName(a.getWarehouseName())
                .auditType(a.getAuditType())
                .status(a.getStatus())
                .totalItems(a.getTotalItems())
                .totalDiscrepancyQty(a.getTotalDiscrepancyQty())
                .createdBy(a.getCreatedBy())
                .approvedBy(a.getApprovedBy())
                .startedAt(a.getStartedAt())
                .completedAt(a.getCompletedAt())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .items(itemDtos)
                .build();
    }
}
