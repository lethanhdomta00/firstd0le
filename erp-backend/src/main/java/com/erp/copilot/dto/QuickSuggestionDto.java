package com.erp.copilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickSuggestionDto {
    private String id;
    private String label;
    private String prompt;
    private String category; // SALES, INVENTORY, DEBT, EXPORT, GENERAL
    private String icon;
}
