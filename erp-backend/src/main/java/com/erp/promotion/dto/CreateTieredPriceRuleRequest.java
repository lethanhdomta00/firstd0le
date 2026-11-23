package com.erp.promotion.dto;

import com.erp.promotion.model.PricingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTieredPriceRuleRequest {

    private Long productId; // null nếu áp dụng toàn ngành

    @NotBlank(message = "Tên bậc giá không được để trống")
    private String tierName;

    @NotNull(message = "Số lượng tối thiểu không được để trống")
    private Integer minQuantity;

    private Integer maxQuantity;

    @NotNull(message = "Loại định giá không được để trống")
    private PricingType pricingType;

    private BigDecimal discountPercent;

    private BigDecimal fixedPrice;

    private Boolean active;
}
