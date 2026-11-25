package com.erp.workflow.dto;

import com.erp.workflow.model.ApprovalDocumentType;
import com.erp.workflow.model.ApprovalStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestDto {

    private Long id;
    private String requestCode;
    private ApprovalDocumentType documentType;
    private String documentTypeLabel;
    private Long documentId;
    private String documentCode;
    private String title;
    private BigDecimal amount;
    private String currency;
    private Integer currentLevel;
    private Integer totalLevels;
    private ApprovalStatus status;
    private String statusLabel;
    private String currentApproverRole;
    private String requesterUsername;
    private String requesterFullName;
    private String oneTimeToken;
    private LocalDateTime tokenExpiresAt;
    private String decisionReason;
    private String payloadSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ApprovalStepHistoryDto> stepHistories;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApprovalStepHistoryDto {
        private Long id;
        private Integer stepLevel;
        private String approverUsername;
        private String approverFullName;
        private String approverRole;
        private String action;
        private String comment;
        private LocalDateTime actionTime;
    }
}
