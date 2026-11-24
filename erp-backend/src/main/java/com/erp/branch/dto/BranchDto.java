package com.erp.branch.dto;

import com.erp.branch.model.Branch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDto {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private String status;
    private Boolean isHeadquarter;
    private Integer warehouseCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BranchDto fromEntity(Branch b, int whCount) {
        return BranchDto.builder()
                .id(b.getId())
                .code(b.getCode())
                .name(b.getName())
                .address(b.getAddress())
                .phone(b.getPhone())
                .status(b.getStatus())
                .isHeadquarter(b.getIsHeadquarter())
                .warehouseCount(whCount)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
