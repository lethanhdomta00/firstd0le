package com.erp.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_step_histories", indexes = {
        @Index(name = "idx_step_req_id", columnList = "approval_request_id"),
        @Index(name = "idx_step_action_time", columnList = "actionTime")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalStepHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    @JsonIgnore
    private ApprovalRequest approvalRequest;

    @Column(nullable = false)
    private Integer stepLevel;

    @Column(nullable = false, length = 50)
    private String approverUsername;

    @Column(length = 100)
    private String approverFullName;

    @Column(length = 50)
    private String approverRole;

    @Column(nullable = false, length = 30)
    private String action; // APPROVE, REJECT, AUTO_APPROVE, SUBMIT

    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime actionTime;
}
