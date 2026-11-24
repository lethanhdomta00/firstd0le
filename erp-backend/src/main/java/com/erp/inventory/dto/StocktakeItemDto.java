package com.erp.inventory.dto;

import com.erp.inventory.model.StocktakeItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StocktakeItemDto {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Integer systemStock;
    private Integer countedStock;
    private Integer differenceQty;
    private String reason;

    public static StocktakeItemDto fromEntity(StocktakeItem item) {
        return StocktakeItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productSku(item.getProductSku())
                .productName(item.getProductName())
                .systemStock(item.getSystemStock())
                .countedStock(item.getCountedStock())
                .differenceQty(item.getDifferenceQty())
                .reason(item.getReason())
                .build();
    }
}
