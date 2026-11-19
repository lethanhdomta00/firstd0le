package com.erp.copilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionResult {
    private String toolName;
    private boolean success;
    private String message;
    private Map<String, Object> data;
    private UiWidgetDto uiWidget;

    public static ToolExecutionResult success(String toolName, String message, Map<String, Object> data, UiWidgetDto uiWidget) {
        return ToolExecutionResult.builder()
                .toolName(toolName)
                .success(true)
                .message(message)
                .data(data)
                .uiWidget(uiWidget)
                .build();
    }

    public static ToolExecutionResult failure(String toolName, String message) {
        return ToolExecutionResult.builder()
                .toolName(toolName)
                .success(false)
                .message(message)
                .build();
    }
}
