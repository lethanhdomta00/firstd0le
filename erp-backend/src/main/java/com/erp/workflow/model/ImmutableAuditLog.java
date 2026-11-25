package com.erp.workflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_trace_id", columnList = "traceId"),
        @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
        @Index(name = "idx_audit_username", columnList = "username"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImmutableAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String traceId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 50)
    private String userRole;

    @Column(nullable = false, length = 50)
    private String action; // CREATE, UPDATE, APPROVE, REJECT, CANCEL, AUTO_APPROVE

    @Column(nullable = false, length = 50)
    private String entityType; // PURCHASE_ORDER, PAYMENT_VOUCHER, SPECIAL_DISCOUNT, APPROVAL_REQUEST

    private Long entityId;

    @Column(length = 50)
    private String entityCode;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String oldValueJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String newValueJson;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
