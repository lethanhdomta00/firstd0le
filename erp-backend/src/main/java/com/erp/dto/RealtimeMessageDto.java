package com.erp.dto;

import com.erp.model.MessageStatus;
import com.erp.model.MessageType;
import com.erp.model.RealtimeMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeMessageDto {
    private Long id;
    private String roomId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;
    private String recipientUsername;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private LocalDateTime createdAt;

    public static RealtimeMessageDto fromEntity(RealtimeMessage entity) {
        if (entity == null) return null;
        return RealtimeMessageDto.builder()
                .id(entity.getId())
                .roomId(entity.getRoomId())
                .senderUsername(entity.getSenderUsername())
                .senderFullName(entity.getSenderFullName())
                .senderAvatarUrl(entity.getSenderAvatarUrl())
                .recipientUsername(entity.getRecipientUsername())
                .content(entity.getContent())
                .type(entity.getType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

