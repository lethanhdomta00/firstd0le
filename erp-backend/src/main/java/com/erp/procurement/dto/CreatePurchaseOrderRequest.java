package com.erp.procurement.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePurchaseOrderRequest {
    @NotNull(message = "Nhà cung cấp không được để trống")
    private Long supplierId;

    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;

    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private String notes;

    @NotEmpty(message = "Danh sách mặt hàng đặt mua không được để trống")
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull(message = "Mã sản phẩm không được để trống")
        private Long productId;

        @NotNull(message = "Số lượng đặt mua không được để trống")
        private Integer orderedQuantity;

        @NotNull(message = "Đơn giá nhập không được để trống")
        private BigDecimal unitPrice;
    }
}
