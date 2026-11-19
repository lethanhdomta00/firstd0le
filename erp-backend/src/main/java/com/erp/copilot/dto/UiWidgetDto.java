package com.erp.copilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UiWidgetDto {
    /**
     * Loại widget:
     * - FILE_DOWNLOAD_CARD
     * - STOCK_ALERT_CARD
     * - ACTION_CONFIRMATION_CARD
     * - SHIPMENT_TRACKING_CARD
     * - MINI_SPARKLINE_CHART
     */
    private String type;

    /**
     * Dữ liệu linh hoạt của widget cho Frontend render
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
}
