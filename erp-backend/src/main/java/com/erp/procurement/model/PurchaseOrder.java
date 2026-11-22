package com.erp.procurement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_code", columnList = "poCode", unique = true),
        @Index(name = "idx_po_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_po_status", columnList = "status"),
        @Index(name = "idx_po_order_date", columnList = "orderDate"),
        @Index(name = "idx_po_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String poCode; // PO-20261122-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    @Column(nullable = false, length = 150)
    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(nullable = false)
    private LocalDate orderDate;

    private LocalDate expectedDeliveryDate;

    /**
     * Tổng tiền hàng trước thuế và chiết khấu
     */
    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Tiền chiết khấu thương mại từ NCC
     */
    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Tiền thuế giá trị gia tăng (VAT)
     */
    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Tổng số tiền thanh toán thực tế = totalAmount - discountAmount + taxAmount
     */
    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal finalAmount = BigDecimal.ZERO;

    /**
     * Số tiền đã thanh toán cho NCC
     */
    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * Số tiền còn nợ cho đơn này = finalAmount - paidAmount
     */
    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @Column(length = 500)
    private String notes;

    @Column(length = 80)
    private String creatorUsername;

    @Column(length = 80)
    private String approverUsername;

    private LocalDateTime approvedAt;

    @Version
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (PurchaseOrderItem item : items) {
                if (item.getSubtotal() != null) {
                    sum = sum.add(item.getSubtotal());
                }
            }
        }
        this.totalAmount = sum;
        BigDecimal disc = this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO;
        BigDecimal tax = this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO;
        this.finalAmount = sum.subtract(disc).add(tax);
        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }
        BigDecimal paid = this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO;
        this.remainingAmount = this.finalAmount.subtract(paid);
    }
}
