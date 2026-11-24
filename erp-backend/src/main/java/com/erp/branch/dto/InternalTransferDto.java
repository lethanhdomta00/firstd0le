package com.erp.branch.dto;

import com.erp.branch.model.InternalTransfer;
import com.erp.branch.model.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalTransferDto {
    private Long id;
    private String transferCode;
    private Long fromWarehouseId;
    private String fromWarehouseName;
    private Long toWarehouseId;
    private String toWarehouseName;
    private TransferStatus status;
    private Integer totalQuantity;
    private String createdBy;
    private String transferredBy;
    private String receivedBy;
    private LocalDateTime departureDate;
    private LocalDateTime arrivalDate;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private List<InternalTransferItemDto> items = new ArrayList<>();

    public static InternalTransferDto fromEntity(InternalTransfer t) {
        List<InternalTransferItemDto> itemDtos = new ArrayList<>();
        if (t.getItems() != null) {
            itemDtos = t.getItems().stream()
                    .map(InternalTransferItemDto::fromEntity)
                    .toList();
        }
        return InternalTransferDto.builder()
                .id(t.getId())
                .transferCode(t.getTransferCode())
                .fromWarehouseId(t.getFromWarehouseId())
                .fromWarehouseName(t.getFromWarehouseName())
                .toWarehouseId(t.getToWarehouseId())
                .toWarehouseName(t.getToWarehouseName())
                .status(t.getStatus())
                .totalQuantity(t.getTotalQuantity())
                .createdBy(t.getCreatedBy())
                .transferredBy(t.getTransferredBy())
                .receivedBy(t.getReceivedBy())
                .departureDate(t.getDepartureDate())
                .arrivalDate(t.getArrivalDate())
                .note(t.getNote())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .items(itemDtos)
                .build();
    }
}
