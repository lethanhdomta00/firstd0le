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
@Table(name = "chat_group_members", indexes = {
    @Index(name = "idx_cgm_group", columnList = "group_id"),
    @Index(name = "idx_cgm_user", columnList = "username"),
    @Index(name = "idx_cgm_group_user", columnList = "group_id, username", unique = true)
})
public class ChatGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "MEMBER"; // "ADMIN" hoặc "MEMBER"

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.role == null) {
            this.role = "MEMBER";
        }
    }
}

