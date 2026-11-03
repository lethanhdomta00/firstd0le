package com.erp.dto;

import com.erp.model.MessageStatus;
import com.erp.model.MessageType;
import com.erp.model.RealtimeMessage;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class RealtimeMessageDto {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long id;
    private String roomId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;
    private String recipientUsername;
    private String content;
    private MessageType type;
    private MessageStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // Trả lời tin nhắn (Reply)
    private Long replyToId;
    private String replyToSender;
    private String replyToContent;

    // Reactions: Map của emoji -> danh sách username đã thả reaction, ví dụ: {"👍": ["admin", "nhanvien"]}
    private Map<String, List<String>> reactions;

    // Xóa & Chỉnh sửa
    private Boolean isDeleted;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;

    private Boolean isEdited;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime editedAt;

    // Ghim tin nhắn (Pinned)
    private Boolean isPinned;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime pinnedAt;

    private String pinnedBy;

    public static RealtimeMessageDto fromEntity(RealtimeMessage entity) {
        if (entity == null) return null;

        Map<String, List<String>> parsedReactions = Collections.emptyMap();
        if (entity.getReactionsJson() != null && !entity.getReactionsJson().trim().isEmpty()) {
            try {
                parsedReactions = OBJECT_MAPPER.readValue(entity.getReactionsJson(), new TypeReference<Map<String, List<String>>>() {});
            } catch (Exception e) {
                log.warn("Không thể parse reactionsJson từ tin nhắn id {}: {}", entity.getId(), e.getMessage());
            }
        }

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
                .replyToId(entity.getReplyToId())
                .replyToSender(entity.getReplyToSender())
                .replyToContent(entity.getReplyToContent())
                .reactions(parsedReactions)
                .isDeleted(entity.getIsDeleted())
                .deletedAt(entity.getDeletedAt())
                .isEdited(entity.getIsEdited())
                .editedAt(entity.getEditedAt())
                .isPinned(Boolean.TRUE.equals(entity.getIsPinned()))
                .pinnedAt(entity.getPinnedAt())
                .pinnedBy(entity.getPinnedBy())
                .build();
    }
}
