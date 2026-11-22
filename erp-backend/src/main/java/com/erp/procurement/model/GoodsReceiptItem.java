package com.erp.procurement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "goods_receipt_items", indexes = {
        @Index(name = "idx_gri_grn_id", columnList = "goods_receipt_note_id"),
        @Index(name = "idx_gri_product_id", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_note_id", nullable = false)
    @JsonIgnore
    private GoodsReceiptNote goodsReceiptNote;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_sku", nullable = false, length = 80)
    private String productSku;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Integer quantityReceived; // Số lượng thực tế nhập kho đợt này

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal importUnitPrice; // Đơn giá nhập đợt này

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal; // Thành tiền đợt này = quantityReceived * importUnitPrice

    // Lưu vết thuật toán Moving Weighted Average Cost
    @Column(precision = 18, scale = 2)
    private BigDecimal oldCostPrice; // Giá vốn cũ trước khi nhập

    @Column(precision = 18, scale = 2)
    private BigDecimal newCostPrice; // Giá vốn bình quân mới sau khi nhập

    private Integer oldPhysicalStock; // Tồn kho vật lý cũ
    private Integer newPhysicalStock; // Tồn kho vật lý mới
}
