package com.erp.inventory.dto;

import com.erp.inventory.model.SerialStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateSerialStatusRequest {
    @NotNull(message = "Trạng thái mới không được để trống")
    private SerialStatus newStatus;

    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long orderId;
    private String orderCode;
    private LocalDate warrantyStartDate;
    private Integer warrantyMonths;
    private String notes;
}
