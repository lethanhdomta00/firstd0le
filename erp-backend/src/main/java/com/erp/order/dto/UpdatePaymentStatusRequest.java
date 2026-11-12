package com.erp.order.dto;

import com.erp.order.model.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePaymentStatusRequest {
    @NotNull(message = "Trạng thái thanh toán không được để trống")
    private PaymentStatus paymentStatus;
}
