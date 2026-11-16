package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_sku", columnList = "sku"),
    @Index(name = "idx_products_barcode", columnList = "barcode")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 64)
    private String sku;

    @Column(length = 64)
    private String barcode;

    @Column(name = "base_unit", length = 32)
    private String baseUnit = "Cái";

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "wholesale_price", precision = 18, scale = 2)
    private BigDecimal wholesalePrice = BigDecimal.ZERO;

    @Column(name = "vip_price", precision = 18, scale = 2)
    private BigDecimal vipPrice = BigDecimal.ZERO;

    @Column(name = "moving_avg_cost", precision = 18, scale = 2)
    private BigDecimal movingAvgCost = BigDecimal.ZERO;

    /**
     * Tồn khả dụng (Available Stock) = Physical Stock - Reserved Stock
     * Trường này giữ tương thích ngược 100% với toàn bộ logic cũ.
     */
    @Column(nullable = false)
    private Integer stock = 0;

    /**
     * Tồn thực tế vật lý nằm trên kệ trong kho (Physical Stock)
     */
    @Column(name = "physical_stock")
    private Integer physicalStock;

    /**
     * Tồn giữ chỗ đang chờ xuất kho trong các đơn hàng PENDING (Reserved Stock)
     */
    @Column(name = "reserved_stock")
    private Integer reservedStock = 0;

    /**
     * Hàng đang trên đường về từ Nhà cung cấp (Incoming Stock)
     */
    @Column(name = "incoming_stock")
    private Integer incomingStock = 0;

    @Column(name = "min_stock_alert")
    private Integer minStockAlert = 10;

    @Column(name = "image_url")
    private String imageUrl;

    @Version
    private Long version = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        syncStockFields();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncStockFields();
    }

    /**
     * Đồng bộ tính toán tồn khả dụng:
     * Available Stock = Physical Stock - Reserved Stock
     */
    public void syncStockFields() {
        if (this.physicalStock == null && this.stock != null) {
            this.physicalStock = this.stock;
        } else if (this.physicalStock != null && this.stock == null) {
            this.stock = this.physicalStock;
        } else if (this.physicalStock == null && this.stock == null) {
            this.physicalStock = 0;
            this.stock = 0;
        }
        if (this.reservedStock == null) {
            this.reservedStock = 0;
        }
        if (this.incomingStock == null) {
            this.incomingStock = 0;
        }
        if (this.movingAvgCost == null) {
            this.movingAvgCost = (this.price != null) ? this.price : BigDecimal.ZERO;
        }
        int physical = (this.physicalStock != null) ? this.physicalStock : 0;
        int reserved = (this.reservedStock != null) ? this.reservedStock : 0;
        this.stock = Math.max(0, physical - reserved);
    }

    public void setStock(Integer stock) {
        this.stock = stock;
        if (this.physicalStock == null) {
            this.physicalStock = (stock != null) ? stock : 0;
        }
    }

    public void setPhysicalStock(Integer physicalStock) {
        this.physicalStock = physicalStock;
        int res = (this.reservedStock != null) ? this.reservedStock : 0;
        this.stock = Math.max(0, (physicalStock != null ? physicalStock : 0) - res);
    }

    public Integer getAvailableStock() {
        int physical = (this.physicalStock != null) ? this.physicalStock : 0;
        int reserved = (this.reservedStock != null) ? this.reservedStock : 0;
        return Math.max(0, physical - reserved);
    }
}
