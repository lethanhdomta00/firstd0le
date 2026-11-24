package com.erp.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryRadarSummaryDto {
    private int totalActiveBatches;
    private int redCount;     // Nguy cấp
    private int yellowCount;  // Cần lưu ý
    private int greenCount;   // An toàn
    private int expiredCount; // Đã quá hạn
}
