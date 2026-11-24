package com.erp.branch.model;

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
@Table(name = "internal_transfers", indexes = {
    @Index(name = "idx_itn_code", columnList = "transfer_code"),
    @Index(name = "idx_itn_status", columnList = "status"),
    @Index(name = "idx_itn_from_wh", columnList = "from_warehouse_id"),
    @Index(name = "idx_itn_to_wh", columnList = "to_warehouse_id"),
    @Index(name = "idx_itn_created_at", columnList = "created_at")
})
public class InternalTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_code", unique = true, nullable = false, length = 40)
    private String transferCode;

    @Column(name = "from_warehouse_id", nullable = false)
    private Long fromWarehouseId;

    @Column(name = "from_warehouse_name", length = 150)
    private String fromWarehouseName;

    @Column(name = "to_warehouse_id", nullable = false)
    private Long toWarehouseId;

    @Column(name = "to_warehouse_name", length = 150)
    private String toWarehouseName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status = TransferStatus.DRAFT;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity = 0;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "transferred_by", length = 100)
    private String transferredBy;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "departure_date")
    private LocalDateTime departureDate;

    @Column(name = "arrival_date")
    private LocalDateTime arrivalDate;

    @Column(length = 1000)
    private String note;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<InternalTransferItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addItem(InternalTransferItem item) {
        items.add(item);
        item.setTransfer(this);
    }

    public void removeItem(InternalTransferItem item) {
        items.remove(item);
        item.setTransfer(null);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TransferStatus.DRAFT;
        }
        if (this.totalQuantity == null) {
            this.totalQuantity = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
