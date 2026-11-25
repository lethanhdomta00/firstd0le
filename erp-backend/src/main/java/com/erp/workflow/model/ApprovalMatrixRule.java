package com.erp.workflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_matrix_rules", indexes = {
        @Index(name = "idx_rule_doc_type", columnList = "documentType"),
        @Index(name = "idx_rule_active", columnList = "active"),
        @Index(name = "idx_rule_level", columnList = "level"),
        @Index(name = "idx_rule_min_amount", columnList = "minAmount")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalMatrixRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApprovalDocumentType documentType;

    /**
     * Hạn mức số tiền tối thiểu để áp dụng quy tắc này (VND)
     */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal minAmount;

    /**
     * Hạn mức số tiền tối đa (nếu null là không giới hạn trên)
     */
    @Column(precision = 18, scale = 2)
    private BigDecimal maxAmount;

    /**
     * Cấp độ phê duyệt (1: Trưởng phòng, 2: Kế toán trưởng/CFO, 3: CEO)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    /**
     * Vai trò có thẩm quyền phê duyệt ở cấp này (e.g. ROLE_MANAGER, ROLE_ACCOUNTANT, ROLE_ADMIN)
     */
    @Column(nullable = false, length = 50)
    private String approverRole;

    /**
     * Có tự động duyệt luôn không nếu rơi vào quy tắc này (e.g. dưới 10 triệu)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean autoApprove = false;

    /**
     * Thứ tự ưu tiên kiểm tra
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
