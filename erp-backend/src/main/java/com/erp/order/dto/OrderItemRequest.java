package com.erp.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private Integer quantity;

    /**
     * Đơn vị tính bán hàng (UoM): Cái, Hộp, Thùng...
     */
    private String uomName;

    /**
     * Hệ số quy đổi ra đơn vị cơ sở (VD: 1 Thùng = 100 Cái)
     * Mặc định là 1 nếu để trống.
     */
    private Integer uomConversionRate;

    /**
     * Đơn giá tùy chỉnh / giá theo UoM (nếu có thỏa thuận)
     */
    private BigDecimal unitPrice;
}
