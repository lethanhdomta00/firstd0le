package com.erp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingTrackingDto {
    private Long orderId;
    private String orderCode;
    private String carrier;
    private String carrierName;
    private String trackingCode;
    private String shippingStatus;
    private String shippingStatusLabel;
    private BigDecimal shippingFee;
    private BigDecimal codAmount;
    private LocalDateTime dispatchedAt;
    private String senderAddress;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private List<TrackingEventDto> events;
}
