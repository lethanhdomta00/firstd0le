package com.erp.order.dto;

import com.erp.order.model.InventoryMovementType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryLogDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private InventoryMovementType movementType;
    private String movementTypeLabel;
    private Integer quantityDelta;
    private Integer stockBefore;
    private Integer stockAfter;
    private String referenceCode;
    private String note;
    private String actorUsername;
    private LocalDateTime createdAt;
}
