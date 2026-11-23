package com.erp.promotion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdjustLoyaltyPointsRequest {

    @NotNull(message = "Mã khách hàng không được để trống")
    private Long customerId;

    @NotNull(message = "Số điểm biến động không được để trống")
    private Integer points; // Dương: cộng thêm, Âm: trừ bớt

    private String reason;
}
