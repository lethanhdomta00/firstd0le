package com.erp.dto;

import com.erp.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private String role; // "USER" hoặc "ASSISTANT"
    private String content;
    private LocalDateTime createdAt;

    public static ChatMessageDto fromEntity(ChatMessage entity) {
        if (entity == null) return null;
        return ChatMessageDto.builder()
                .id(entity.getId())
                .role(entity.getRole())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

