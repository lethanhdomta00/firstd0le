package com.erp.branch.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateTransferRequest {
    @NotNull(message = "Kho xuất không được để trống")
    private Long fromWarehouseId;

    @NotNull(message = "Kho nhận không được để trống")
    private Long toWarehouseId;

    private String note;

    @NotEmpty(message = "Danh sách sản phẩm điều chuyển không được để trống")
    private List<CreateTransferItemRequest> items;

    @Data
    public static class CreateTransferItemRequest {
        @NotNull(message = "Sản phẩm không được để trống")
        private Long productId;

        @NotNull(message = "Số lượng điều chuyển phải lớn hơn 0")
        private Integer quantity;

        private String note;
    }
}
