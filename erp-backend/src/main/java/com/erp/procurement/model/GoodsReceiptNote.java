package com.erp.procurement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipt_notes", indexes = {
        @Index(name = "idx_grn_code", columnList = "grnCode", unique = true),
        @Index(name = "idx_grn_po_id", columnList = "purchase_order_id"),
        @Index(name = "idx_grn_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_grn_receipt_date", columnList = "receiptDate"),
        @Index(name = "idx_grn_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String grnCode; // GRN-20261122-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "items"})
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    @Column(nullable = false)
    private LocalDateTime receiptDate;

    @Column(length = 100)
    @Builder.Default
    private String warehouseLocation = "Kho Chính Trung Tâm";

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalReceiptAmount = BigDecimal.ZERO;

    @Column(length = 80)
    private String receivedByUsername; // Thủ kho thực hiện kiểm nhận

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "goodsReceiptNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceiptItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
