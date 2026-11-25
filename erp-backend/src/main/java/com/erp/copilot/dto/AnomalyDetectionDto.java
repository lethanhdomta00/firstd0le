package com.erp.copilot.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetectionDto {

    private LocalDateTime scannedAt;
    private Integer totalAlerts;
    private Integer criticalCount;
    private List<AnomalyItemDto> alerts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnomalyItemDto {
        private String id;
        private String category; // ABNORMAL_DISCOUNT, CASH_REVENUE_MISMATCH, BURN_RATE_SPIKE
        private String severity; // CRITICAL, HIGH, MEDIUM
        private String title;
        private String description;
        private BigDecimal impactAmount;
        private String entityReference;
        private String recommendation;
        private String actionButtonLabel;
    }
}
