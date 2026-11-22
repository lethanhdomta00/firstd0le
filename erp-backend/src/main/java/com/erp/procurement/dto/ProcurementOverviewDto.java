package com.erp.procurement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementOverviewDto {
    private long totalSuppliers;
    private long activeSuppliers;
    private long pendingPurchaseOrders;
    private long approvedPurchaseOrders;
    private BigDecimal totalAccountsPayable; // Tổng công nợ phải trả NCC
    private BigDecimal totalPurchasedThisMonth; // Tổng giá trị mua hàng trong tháng
    private BigDecimal totalRemainingPoDebt; // Tổng nợ theo đơn PO
}
