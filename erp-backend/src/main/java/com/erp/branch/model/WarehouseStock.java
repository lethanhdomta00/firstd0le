package com.erp.branch.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "warehouse_stocks", indexes = {
    @Index(name = "idx_wh_stock_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_wh_stock_product", columnList = "product_id"),
    @Index(name = "idx_wh_stock_sku", columnList = "product_sku"),
    @Index(name = "idx_wh_stock_branch", columnList = "branch_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_wh_product", columnNames = {"warehouse_id", "product_id"})
})
public class WarehouseStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_name", length = 150)
    private String warehouseName;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_sku", length = 64)
    private String productSku;

    @Column(name = "product_name", length = 255)
    private String productName;

    /**
     * Tồn thực tế vật lý nằm trong kho
     */
    @Column(name = "physical_stock", nullable = false)
    private Integer physicalStock = 0;

    /**
     * Tồn khả dụng có thể bán = physicalStock - reservedStock
     */
    @Column(name = "available_stock", nullable = false)
    private Integer availableStock = 0;

    /**
     * Hàng đã giữ chỗ cho đơn bán chờ xuất
     */
    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;

    /**
     * Hàng đang điều chuyển tới kho này (In-transit)
     */
    @Column(name = "in_transit_stock", nullable = false)
    private Integer inTransitStock = 0;

    /**
     * Định mức tối thiểu cảnh báo hết hàng
     */
    @Column(name = "min_stock_alert", nullable = false)
    private Integer minStockAlert = 10;

    @Version
    private Long version;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (this.physicalStock == null) this.physicalStock = 0;
        if (this.reservedStock == null) this.reservedStock = 0;
        if (this.inTransitStock == null) this.inTransitStock = 0;
        if (this.minStockAlert == null) this.minStockAlert = 10;
        this.availableStock = Math.max(0, this.physicalStock - this.reservedStock);
        this.updatedAt = LocalDateTime.now();
    }
}
