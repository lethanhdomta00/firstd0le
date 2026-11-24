package com.erp.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product_serials", indexes = {
    @Index(name = "idx_serial_number", columnList = "serial_number"),
    @Index(name = "idx_serial_product", columnList = "product_id"),
    @Index(name = "idx_serial_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_serial_status", columnList = "status"),
    @Index(name = "idx_serial_order", columnList = "order_id")
})
public class ProductSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serial_number", unique = true, nullable = false, length = 64)
    private String serialNumber;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_sku", length = 64)
    private String productSku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name", length = 150)
    private String warehouseName;

    @Column(name = "batch_id")
    private Long batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SerialStatus status = SerialStatus.IN_STOCK;

    // Customer association
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    // Order association
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code", length = 40)
    private String orderCode;

    // Warranty tracking
    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "warranty_months")
    private Integer warrantyMonths = 12;

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
        if (this.status == null) this.status = SerialStatus.IN_STOCK;
        if (this.warrantyMonths == null) this.warrantyMonths = 12;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
