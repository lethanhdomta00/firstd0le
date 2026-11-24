package com.erp.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RecordCountRequest {
    @NotEmpty(message = "Danh sách kiểm đếm không được để trống")
    private List<CountItemRequest> counts;

    private String notes;

    @Data
    public static class CountItemRequest {
        private Long itemId;
        private Long productId;
        private Integer countedStock;
        private String reason;
    }
}
