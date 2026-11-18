package com.erp.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchCarrierRequest {
    @NotBlank(message = "Đơn vị vận chuyển không được để trống")
    private String carrier; // GHN, GHTK, VIETTEL_POST, EXPRESS

    private BigDecimal shippingFee;
    private BigDecimal codAmount;
    private String note;
}
