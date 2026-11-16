package com.erp.order.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "erp_order_items", indexes = {
    @Index(name = "idx_order_items_order_id", columnList = "order_id"),
    @Index(name = "idx_order_items_product_id", columnList = "product_id")
})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 80)
    private String productSku;

    @Column(name = "product_image_url", columnDefinition = "TEXT")
    private String productImageUrl;

    /**
     * Đơn vị tính bán hàng (UoM): Cái, Hộp, Thùng, Lon...
     */
    @Column(name = "uom_name", length = 32)
    private String uomName = "Cái";

    /**
     * Hệ số quy đổi ra đơn vị cơ sở (VD: 1 Thùng = 24 Cái)
     */
    @Column(name = "uom_conversion_rate", precision = 10, scale = 2)
    private BigDecimal uomConversionRate = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    /**
     * Giá vốn đóng băng tại thời điểm bán (Snapshot Cost Price - Moving Average Cost)
     * Đảm bảo báo cáo lợi nhuận quá khứ không bị sai lệch khi giá nhập hàng sau này biến động.
     */
    @Column(name = "snapshot_cost_price", precision = 18, scale = 2)
    private BigDecimal snapshotCostPrice = BigDecimal.ZERO;

    /**
     * Lợi nhuận gộp của dòng này: lineTotal - (snapshotCostPrice * quantity)
     */
    @Column(name = "gross_profit", precision = 18, scale = 2)
    private BigDecimal grossProfit = BigDecimal.ZERO;

    /**
     * Tự động tính toán Lợi nhuận gộp của dòng sản phẩm
     */
    public void calculateGrossProfit() {
        if (lineTotal == null) {
            lineTotal = (unitPrice != null ? unitPrice : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(quantity != null ? quantity : 0))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal cost = (snapshotCostPrice != null ? snapshotCostPrice : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(quantity != null ? quantity : 0));
        this.grossProfit = lineTotal.subtract(cost).setScale(2, RoundingMode.HALF_UP);
    }
}
