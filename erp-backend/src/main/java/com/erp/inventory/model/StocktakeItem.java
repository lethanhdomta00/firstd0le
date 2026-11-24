package com.erp.inventory.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "audit")
@Entity
@Table(name = "stocktake_items", indexes = {
    @Index(name = "idx_stocktake_item_audit", columnList = "audit_id"),
    @Index(name = "idx_stocktake_item_product", columnList = "product_id")
})
public class StocktakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    @JsonBackReference
    private StocktakeAudit audit;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_sku", length = 64)
    private String productSku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "system_stock", nullable = false)
    private Integer systemStock = 0;

    @Column(name = "counted_stock", nullable = false)
    private Integer countedStock = 0;

    @Column(name = "difference_qty", nullable = false)
    private Integer differenceQty = 0;

    @Column(length = 500)
    private String reason;

    public void calculateDifference() {
        int sys = this.systemStock != null ? this.systemStock : 0;
        int count = this.countedStock != null ? this.countedStock : 0;
        this.differenceQty = count - sys;
    }
}
