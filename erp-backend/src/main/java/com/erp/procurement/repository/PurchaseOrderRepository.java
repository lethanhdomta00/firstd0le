package com.erp.procurement.repository;

import com.erp.procurement.model.PurchaseOrder;
import com.erp.procurement.model.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoCode(String poCode);

    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.items WHERE po.id = :id")
    Optional<PurchaseOrder> findWithItemsById(@Param("id") Long id);

    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.items WHERE po.poCode = :poCode")
    Optional<PurchaseOrder> findWithItemsByPoCode(@Param("poCode") String poCode);

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "(:keyword IS NULL OR LOWER(po.poCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(po.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:startDate IS NULL OR po.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR po.orderDate <= :endDate)")
    Page<PurchaseOrder> searchPurchaseOrders(
            @Param("keyword") String keyword,
            @Param("status") PurchaseOrderStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    long countByStatus(PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(po.finalAmount), 0) FROM PurchaseOrder po WHERE po.orderDate >= :since AND po.status <> 'CANCELLED'")
    BigDecimal sumTotalPurchaseSince(@Param("since") LocalDate since);

    @Query("SELECT COALESCE(SUM(po.remainingAmount), 0) FROM PurchaseOrder po WHERE po.status IN ('APPROVED', 'PARTIALLY_RECEIVED', 'COMPLETED')")
    BigDecimal sumTotalRemainingDebt();

    List<PurchaseOrder> findTop10ByOrderByCreatedAtDesc();
}
