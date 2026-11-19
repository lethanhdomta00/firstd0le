package com.erp.copilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotResponseDto {
    private String content;
    private String tier; // TIER_1_ACTION, TIER_2_ADVISORY, TIER_3_ERP_QA, TIER_4_CONVERSATIONAL
    @Builder.Default
    private List<ThinkingStepDto> thinkingTrace = new ArrayList<>();
    private UiWidgetDto uiWidget;
    private ToolExecutionResult toolResult;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThinkingStepDto {
        private String step;    // ANALYZE_INTENT, TOOL_CALLING, DB_QUERY, SYNTHESIZE
        private String message;
        private Long elapsedMs;
    }
}
