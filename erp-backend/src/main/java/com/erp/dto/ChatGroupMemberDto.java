package com.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroupMemberDto {
    private Long id;
    private Long groupId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String role; // "ADMIN" | "MEMBER"
    private LocalDateTime joinedAt;
}

