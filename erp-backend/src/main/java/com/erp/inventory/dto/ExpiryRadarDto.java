package com.erp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryRadarDto {
    private ExpiryRadarSummaryDto summary;
    @Builder.Default
    private List<ProductBatchDto> redZone = new ArrayList<>();    // < 30 ngày hoặc quá hạn
    @Builder.Default
    private List<ProductBatchDto> yellowZone = new ArrayList<>(); // 30 - 90 ngày
    @Builder.Default
    private List<ProductBatchDto> greenZone = new ArrayList<>();  // > 90 ngày
}
