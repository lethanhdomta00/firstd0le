package com.erp.procurement.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateGoodsReceiptRequest {
    @NotNull(message = "Mã đơn đặt hàng PO không được để trống")
    private Long purchaseOrderId;

    private String warehouseLocation;
    private String notes;

    @NotEmpty(message = "Danh sách mặt hàng kiểm nhận không được để trống")
    private List<ReceiptItemRequest> items;

    @Data
    public static class ReceiptItemRequest {
        @NotNull(message = "ID dòng PO không được để trống")
        private Long purchaseOrderItemId;

        @NotNull(message = "Số lượng thực nhận không được để trống")
        private Integer quantityReceived;

        private BigDecimal importUnitPrice; // Nếu bỏ trống lấy giá trên PO
    }
}
