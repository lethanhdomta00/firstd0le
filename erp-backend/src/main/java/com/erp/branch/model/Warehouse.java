package com.erp.branch.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "warehouses", indexes = {
    @Index(name = "idx_warehouses_code", columnList = "code"),
    @Index(name = "idx_warehouses_branch_id", columnList = "branch_id"),
    @Index(name = "idx_warehouses_type", columnList = "warehouse_type")
})
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "branch_name", length = 150)
    private String branchName;

    @Column(unique = true, nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_type", nullable = false, length = 30)
    private WarehouseType warehouseType = WarehouseType.RETAIL_STORE;

    @Column(length = 500)
    private String address;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
        if (this.warehouseType == null) {
            this.warehouseType = WarehouseType.RETAIL_STORE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
