package com.erp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDecisionRequest {

    @NotBlank(message = "Hành động phê duyệt không được để trống (APPROVE / REJECT)")
    private String action; // APPROVE hoặc REJECT

    private String comment;
}
