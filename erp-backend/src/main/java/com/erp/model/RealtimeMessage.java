package com.erp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "realtime_messages", indexes = {
    @Index(name = "idx_rt_room", columnList = "room_id"),
    @Index(name = "idx_rt_sender", columnList = "sender_username"),
    @Index(name = "idx_rt_recipient", columnList = "recipient_username"),
    @Index(name = "idx_rt_created", columnList = "created_at")
})
public class RealtimeMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "GLOBAL" cho phòng chat chung toàn công ty, hoặc "usernameA_usernameB" cho chat 1-1 riêng tư
    @Column(name = "room_id", nullable = false, length = 100)
    @Builder.Default
    private String roomId = "GLOBAL";

    @Column(name = "sender_username", nullable = false, length = 50)
    private String senderUsername;

    @Column(name = "sender_full_name", length = 100)
    private String senderFullName;

    @Column(name = "sender_avatar_url", length = 500)
    private String senderAvatarUrl;

    // recipient_username = null nếu là tin nhắn trong phòng chat chung
    @Column(name = "recipient_username", length = 50)
    private String recipientUsername;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20, nullable = false)
    @Builder.Default
    private MessageType type = MessageType.CHAT;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_status", length = 20, nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.roomId == null || this.roomId.trim().isEmpty()) {
            this.roomId = "GLOBAL";
        }
        if (this.type == null) {
            this.type = MessageType.CHAT;
        }
        if (this.status == null) {
            this.status = MessageStatus.SENT;
        }
    }
}

