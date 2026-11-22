package com.erp.procurement.repository;

import com.erp.procurement.model.GoodsReceiptNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Long> {

    Optional<GoodsReceiptNote> findByGrnCode(String grnCode);

    @Query("SELECT grn FROM GoodsReceiptNote grn LEFT JOIN FETCH grn.items WHERE grn.id = :id")
    Optional<GoodsReceiptNote> findWithItemsById(@Param("id") Long id);

    @Query("SELECT grn FROM GoodsReceiptNote grn WHERE " +
           "(:keyword IS NULL OR LOWER(grn.grnCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(grn.purchaseOrder.poCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(grn.supplier.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<GoodsReceiptNote> searchReceiptNotes(@Param("keyword") String keyword, Pageable pageable);

    List<GoodsReceiptNote> findByPurchaseOrderIdOrderByReceiptDateDesc(Long purchaseOrderId);

    List<GoodsReceiptNote> findTop10ByOrderByReceiptDateDesc();
}
