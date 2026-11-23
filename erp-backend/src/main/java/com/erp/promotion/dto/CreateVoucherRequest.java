package com.erp.promotion.dto;

import com.erp.promotion.model.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotBlank(message = "Tên chương trình khuyến mại không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Loại chiết khấu không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị chiết khấu không được để trống")
    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer maxUsage;

    private Integer maxUsagePerCustomer;

    private Boolean active;
}
