package com.erp.order.dto;

import com.erp.order.model.PaymentMethod;
import com.erp.order.model.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String note;

    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal taxRate = BigDecimal.ZERO;

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Valid
    private List<OrderItemRequest> items;
}
