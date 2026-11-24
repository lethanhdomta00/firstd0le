package com.erp.branch.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReceiveTransferRequest {
    private String note;

    @NotEmpty(message = "Danh sách chi tiết kiểm nhận không được để trống")
    private List<ReceiveTransferItemRequest> items;

    @Data
    public static class ReceiveTransferItemRequest {
        private Long itemId; // ID của InternalTransferItem
        private Long productId;
        private Integer receivedQuantity; // Thực nhận nguyên vẹn
        private Integer damagedQuantity;  // Hàng hỏng / rách / lỗi
        private String note;
    }
}
