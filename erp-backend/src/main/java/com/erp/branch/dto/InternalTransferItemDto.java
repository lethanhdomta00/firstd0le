package com.erp.branch.dto;

import com.erp.branch.model.InternalTransferItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalTransferItemDto {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Integer requestedQuantity;
    private Integer transferredQuantity;
    private Integer receivedQuantity;
    private Integer damagedQuantity;
    private String itemNote;

    public static InternalTransferItemDto fromEntity(InternalTransferItem item) {
        return InternalTransferItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productSku(item.getProductSku())
                .productName(item.getProductName())
                .requestedQuantity(item.getRequestedQuantity())
                .transferredQuantity(item.getTransferredQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .damagedQuantity(item.getDamagedQuantity())
                .itemNote(item.getItemNote())
                .build();
    }
}
