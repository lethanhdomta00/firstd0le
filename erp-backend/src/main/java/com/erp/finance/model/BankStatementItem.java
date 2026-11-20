package com.erp.finance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_statement_items", indexes = {
        @Index(name = "idx_statement_trans_code", columnList = "transactionCode"),
        @Index(name = "idx_statement_matched_order", columnList = "matchedOrderCode"),
        @Index(name = "idx_statement_status", columnList = "status"),
        @Index(name = "idx_statement_trans_date", columnList = "transactionDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankStatementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String transactionCode; // Mã giao dịch ngân hàng (FT..., MB..., TraceNo)

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String transactionType; // CR (Thu / Tiền vào), DR (Chi / Tiền ra)

    @Column(nullable = false, length = 500)
    private String description; // Nội dung sao kê gốc: "NGUYEN VAN A CHUYEN KHOAN DON HANG ORD-20261114-1001"

    @Column(length = 50)
    private String bankName; // Vietcombank, MBBank...

    @Column(length = 50)
    private String accountNumber;

    // Kết quả phân tích đối soát thông minh
    @Column(length = 50)
    private String matchedOrderCode; // ORD-...

    private Long matchedOrderId;

    @Column(length = 150)
    private String matchedCustomerName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private ReconciliationStatus status = ReconciliationStatus.PENDING;

    @Column(length = 255)
    private String reconciliationNote;

    private Long createdVoucherId; // ID phiếu thu tự động tạo khi đối soát thành công

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
