package com.erp.order.dto;

import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentMethod;
import com.erp.order.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDto {
    private Long id;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String note;
    private OrderStatus status;
    private String statusLabel;
    private PaymentStatus paymentStatus;
    private String paymentStatusLabel;
    private PaymentMethod paymentMethod;
    private String paymentMethodLabel;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private boolean stockDeducted;
    private String createdByUsername;
    private LocalDateTime confirmedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int totalQuantity;
    private List<OrderItemDto> items;
}
