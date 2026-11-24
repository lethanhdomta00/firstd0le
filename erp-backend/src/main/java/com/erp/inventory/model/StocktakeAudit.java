package com.erp.inventory.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "items")
@Entity
@Table(name = "stocktake_audits", indexes = {
    @Index(name = "idx_stocktake_code", columnList = "audit_code"),
    @Index(name = "idx_stocktake_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_stocktake_status", columnList = "status"),
    @Index(name = "idx_stocktake_created_at", columnList = "created_at")
})
public class StocktakeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_code", unique = true, nullable = false, length = 40)
    private String auditCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_name", length = 150)
    private String warehouseName;

    @Column(name = "audit_type", length = 30)
    private String auditType = "PERIODIC"; // PERIODIC, SPOT_CHECK

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StocktakeStatus status = StocktakeStatus.DRAFT;

    @Column(name = "total_items")
    private Integer totalItems = 0;

    @Column(name = "total_discrepancy_qty")
    private Integer totalDiscrepancyQty = 0;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<StocktakeItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addItem(StocktakeItem item) {
        items.add(item);
        item.setAudit(this);
    }

    public void removeItem(StocktakeItem item) {
        items.remove(item);
        item.setAudit(null);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = StocktakeStatus.DRAFT;
        if (this.totalItems == null) this.totalItems = 0;
        if (this.totalDiscrepancyQty == null) this.totalDiscrepancyQty = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
