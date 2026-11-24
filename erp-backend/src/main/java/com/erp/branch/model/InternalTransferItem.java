package com.erp.branch.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "transfer")
@Entity
@Table(name = "internal_transfer_items", indexes = {
    @Index(name = "idx_itn_item_transfer", columnList = "transfer_id"),
    @Index(name = "idx_itn_item_product", columnList = "product_id")
})
public class InternalTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    @JsonBackReference
    private InternalTransfer transfer;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_sku", length = 64)
    private String productSku;

    @Column(name = "product_name", length = 255)
    private String productName;

    /**
     * Số lượng yêu cầu điều chuyển
     */
    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity = 0;

    /**
     * Số lượng thực xuất từ kho đi
     */
    @Column(name = "transferred_quantity", nullable = false)
    private Integer transferredQuantity = 0;

    /**
     * Số lượng thực nhận tại kho đích
     */
    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    /**
     * Số lượng hư hại / lỗi phát hiện khi kiểm đếm nhận hàng
     */
    @Column(name = "damaged_quantity", nullable = false)
    private Integer damagedQuantity = 0;

    @Column(name = "item_note", length = 500)
    private String itemNote;
}
