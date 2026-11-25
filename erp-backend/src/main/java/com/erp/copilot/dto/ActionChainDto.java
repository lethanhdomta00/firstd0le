package com.erp.copilot.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionChainDto {

    private String chainId;
    private String userPrompt;
    private String title;
    private String summary;
    private List<ActionStepDto> steps;
    private Boolean executed;
    private String executionResult;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionStepDto {
        private Integer stepIndex;
        private String actionType; // CREATE_PO, DRAFT_VOUCHER, TELEGRAM_NOTIFY, INVENTORY_RESERVE
        private String description;
        private String targetModule; // PROCUREMENT, FINANCE, COMMUNICATION, INVENTORY
        private String payloadJson;
        private String status; // PENDING, SUCCESS, FAILED
        private String resultDetails;
    }
}
