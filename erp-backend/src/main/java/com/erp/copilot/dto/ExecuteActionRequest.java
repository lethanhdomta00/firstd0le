package com.erp.copilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteActionRequest {
    private String actionType; // HOLD_CREDIT, DRAFT_EXPENSE, RESTOCK_ORDER, CANCEL_ORDER
    private String targetId;
    private boolean confirmed;
    private Map<String, Object> payload;
}
