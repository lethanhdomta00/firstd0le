package com.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroupDto {
    private Long id;
    private String name;
    private String creatorUsername;
    private String creatorFullName;
    private String avatarUrl;
    private int memberCount;
    private List<ChatGroupMemberDto> members;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private String lastMessageStatus;
    private String lastMessageSender;
    private LocalDateTime createdAt;
}

