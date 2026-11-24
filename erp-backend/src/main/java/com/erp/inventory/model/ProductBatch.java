package com.erp.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product_batches", indexes = {
    @Index(name = "idx_batch_code", columnList = "batch_code"),
    @Index(name = "idx_batch_product", columnList = "product_id"),
    @Index(name = "idx_batch_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_batch_expiry", columnList = "expiry_date"),
    @Index(name = "idx_batch_status", columnList = "status")
})
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_code", unique = true, nullable = false, length = 40)
    private String batchCode;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_sku", length = 64)
    private String productSku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_name", length = 150)
    private String warehouseName;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity = 0;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity = 0;

    @Column(name = "cost_price", precision = 18, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus status = BatchStatus.ACTIVE;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.initialQuantity == null) this.initialQuantity = 0;
        if (this.remainingQuantity == null) this.remainingQuantity = this.initialQuantity;
        if (this.costPrice == null) this.costPrice = BigDecimal.ZERO;
        refreshStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        refreshStatus();
    }

    public void refreshStatus() {
        if (this.remainingQuantity != null && this.remainingQuantity <= 0) {
            this.status = BatchStatus.DEPLETED;
            return;
        }
        if (this.expiryDate != null) {
            LocalDate today = LocalDate.now();
            if (this.expiryDate.isBefore(today)) {
                this.status = BatchStatus.EXPIRED;
            } else if (this.expiryDate.isBefore(today.plusDays(30))) {
                this.status = BatchStatus.NEAR_EXPIRY;
            } else {
                this.status = BatchStatus.ACTIVE;
            }
        }
    }
}
