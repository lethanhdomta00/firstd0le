package com.erp.order.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lịch sử giao dịch thanh toán tự động qua Webhook ngân hàng / VietQR (NAPAS 24/7)
 * Đảm bảo Idempotency (chống trùng lặp callback) qua transaction_id duy nhất.
 */
@Data
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_trans_id", columnList = "transaction_id", unique = true),
    @Index(name = "idx_payment_order_code", columnList = "order_code")
})
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 100)
    private String transactionId;

    @Column(name = "order_code", nullable = false, length = 60)
    private String orderCode;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;

    @Column(length = 500)
    private String note;

    @Column(length = 30)
    private String status = "SUCCESS";

    @Column(name = "received_at")
    private LocalDateTime receivedAt = LocalDateTime.now();
}
