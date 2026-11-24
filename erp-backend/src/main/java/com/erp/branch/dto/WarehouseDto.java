package com.erp.branch.dto;

import com.erp.branch.model.Warehouse;
import com.erp.branch.model.WarehouseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDto {
    private Long id;
    private Long branchId;
    private String branchName;
    private String code;
    private String name;
    private WarehouseType warehouseType;
    private String address;
    private Boolean active;
    private Integer totalSkus;
    private Integer totalPhysicalStock;
    private Integer totalAvailableStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WarehouseDto fromEntity(Warehouse w) {
        return WarehouseDto.builder()
                .id(w.getId())
                .branchId(w.getBranchId())
                .branchName(w.getBranchName())
                .code(w.getCode())
                .name(w.getName())
                .warehouseType(w.getWarehouseType())
                .address(w.getAddress())
                .active(w.getActive())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
