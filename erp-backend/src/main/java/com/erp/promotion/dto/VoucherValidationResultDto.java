package com.erp.promotion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VoucherValidationResultDto {
    private boolean valid;
    private String code;
    private String voucherName;
    private BigDecimal discountAmount;
    private String message;
}
