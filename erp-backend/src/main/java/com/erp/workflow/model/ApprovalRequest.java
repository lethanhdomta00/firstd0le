package com.erp.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_requests", indexes = {
        @Index(name = "idx_apr_req_code", columnList = "requestCode", unique = true),
        @Index(name = "idx_apr_doc_type_id", columnList = "documentType, documentId"),
        @Index(name = "idx_apr_status", columnList = "status"),
        @Index(name = "idx_apr_token", columnList = "oneTimeToken"),
        @Index(name = "idx_apr_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String requestCode; // APR-20261125-00001

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApprovalDocumentType documentType;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false, length = 50)
    private String documentCode; // PO-20261122-001 hoặc PC-001

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(nullable = false)
    @Builder.Default
    private Integer currentLevel = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalLevels = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(length = 50)
    private String currentApproverRole; // ROLE_MANAGER, ROLE_ACCOUNTANT, ROLE_ADMIN

    @Column(nullable = false, length = 50)
    private String requesterUsername;

    @Column(length = 100)
    private String requesterFullName;

    @Column(length = 100, unique = true)
    private String oneTimeToken; // Token 1-click duyệt qua Email / Webhook / Telegram

    private LocalDateTime tokenExpiresAt;

    @Column(length = 500)
    private String decisionReason; // Lý do từ chối hoặc phê duyệt

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payloadSnapshot; // Snapshot JSON thông tin chứng từ gốc

    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("approvalRequest")
    @Builder.Default
    private List<ApprovalStepHistory> stepHistories = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addStepHistory(ApprovalStepHistory step) {
        stepHistories.add(step);
        step.setApprovalRequest(this);
    }
}
