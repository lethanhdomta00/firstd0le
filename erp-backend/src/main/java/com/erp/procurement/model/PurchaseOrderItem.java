package com.erp.procurement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items", indexes = {
        @Index(name = "idx_poi_po_id", columnList = "purchase_order_id"),
        @Index(name = "idx_poi_product_id", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @JsonIgnore
    private PurchaseOrder purchaseOrder;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_sku", nullable = false, length = 80)
    private String productSku;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "unit_name", length = 32)
    @Builder.Default
    private String unitName = "Cái";

    @Column(nullable = false)
    private Integer orderedQuantity; // Số lượng đặt mua

    @Column(nullable = false)
    @Builder.Default
    private Integer receivedQuantity = 0; // Số lượng thực tế đã nhập kho

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice; // Đơn giá nhập thỏa thuận

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal; // Thành tiền = orderedQuantity * unitPrice

    public void calculateSubtotal() {
        if (orderedQuantity != null && unitPrice != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(orderedQuantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }
}
