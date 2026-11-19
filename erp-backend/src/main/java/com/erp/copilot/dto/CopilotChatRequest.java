package com.erp.copilot.dto;

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
public class CopilotChatRequest {
    private String message;
    @Builder.Default
    private List<ChatMessageHistoryDto> conversationHistory = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageHistoryDto {
        private String role; // "user" hoặc "assistant"
        private String content;
    }
}
