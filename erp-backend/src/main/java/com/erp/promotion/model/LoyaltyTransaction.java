package com.erp.promotion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Entity
@Table(name = "loyalty_transactions", indexes = {
    @Index(name = "idx_lt_customer_id", columnList = "customer_id"),
    @Index(name = "idx_lt_order_id", columnList = "order_id"),
    @Index(name = "idx_lt_created_at", columnList = "created_at")
})
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code", length = 40)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoyaltyTransactionType type = LoyaltyTransactionType.EARN;

    /**
     * Số điểm biến động: Dương (+) khi tích lũy / hoàn trả; Âm (-) khi sử dụng
     */
    @Column(nullable = false)
    private Integer points = 0;

    /**
     * Số dư điểm khả dụng của khách hàng ngay sau giao dịch này
     */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter = 0;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
