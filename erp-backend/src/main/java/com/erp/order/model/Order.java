package com.erp.order.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "erp_orders", indexes = {
    @Index(name = "idx_orders_created_at", columnList = "created_at"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_payment_status", columnList = "payment_status"),
    @Index(name = "idx_orders_order_code", columnList = "order_code"),
    @Index(name = "idx_orders_customer_phone", columnList = "customer_phone"),
    @Index(name = "idx_orders_tracking_code", columnList = "tracking_code")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "order_code", unique = true, nullable = false, length = 40)
    private String orderCode;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "customer_email", length = 120)
    private String customerEmail;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "note", length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    /**
     * Bậc giá áp dụng cho đơn: RETAIL, WHOLESALE_L1, VIP
     */
    @Column(name = "price_tier", length = 32)
    private String priceTier = "RETAIL";

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Mã Voucher khuyến mại áp dụng cho đơn
     */
    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    /**
     * Tiền giảm trừ từ mã Voucher
     */
    @Column(name = "voucher_discount", precision = 18, scale = 2)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    /**
     * Số điểm tích lũy thành viên đã dùng để giảm trừ cho đơn
     */
    @Column(name = "loyalty_points_used")
    private Integer loyaltyPointsUsed = 0;

    /**
     * Tiền giảm trừ từ điểm tích lũy thành viên (1 điểm = 1.000 VNĐ)
     */
    @Column(name = "loyalty_discount", precision = 18, scale = 2)
    private BigDecimal loyaltyDiscount = BigDecimal.ZERO;

    /**
     * Số điểm tích lũy được thưởng khi hoàn thành đơn này
     */
    @Column(name = "loyalty_points_earned")
    private Integer loyaltyPointsEarned = 0;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Tổng giá vốn toàn đơn hàng (Total Cost Amount - MAC Snapshot)
     */
    @Column(name = "total_cost_amount", precision = 18, scale = 2)
    private BigDecimal totalCostAmount = BigDecimal.ZERO;

    /**
     * Lợi nhuận gộp thực tế của đơn hàng (Gross Profit)
     * = totalAmount - totalCostAmount
     */
    @Column(name = "gross_profit", precision = 18, scale = 2)
    private BigDecimal grossProfit = BigDecimal.ZERO;

    /**
     * Cờ kiểm soát công nợ (Credit Gatekeeper):
     * Nếu dư nợ + đơn mới vượt hạn mức credit_limit, cờ này bật true
     */
    @Column(name = "is_credit_exceeded")
    private Boolean creditExceeded = false;

    public boolean isCreditExceeded() {
        return Boolean.TRUE.equals(this.creditExceeded);
    }

    /**
     * Trạng thái phê duyệt công nợ: NONE, PENDING, APPROVED, REJECTED
     */
    @Column(name = "credit_approval_status", length = 32)
    private String creditApprovalStatus = "NONE";

    /**
     * Đơn vị vận chuyển logistics: GHN, GHTK, VIETTEL_POST, EXPRESS, SELF_PICKUP
     */
    @Column(name = "shipping_carrier", length = 50)
    private String shippingCarrier;

    /**
     * Mã vận đơn theo dõi (Tracking Code)
     */
    @Column(name = "tracking_code", length = 60)
    private String trackingCode;

    /**
     * Cước phí vận chuyển
     */
    @Column(name = "shipping_fee", precision = 18, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /**
     * Tiền thu hộ COD (Cash on Delivery)
     */
    @Column(name = "cod_amount", precision = 18, scale = 2)
    private BigDecimal codAmount = BigDecimal.ZERO;

    /**
     * Trạng thái giao vận: NONE, READY_TO_SHIP, IN_TRANSIT, DELIVERED, RETURNED
     */
    @Column(name = "shipping_status", length = 30)
    private String shippingStatus = "NONE";

    /**
     * Thời điểm xuất kho / bàn giao cho đơn vị vận chuyển
     */
    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "stock_deducted", nullable = false)
    private boolean stockDeducted = true;

    @Column(name = "created_by_username", length = 80)
    private String createdByUsername;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    public void clearItems() {
        for (OrderItem item : new ArrayList<>(items)) {
            removeItem(item);
        }
    }
}
