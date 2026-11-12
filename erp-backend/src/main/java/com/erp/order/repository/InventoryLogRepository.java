package com.erp.order.repository;

import com.erp.order.model.InventoryLog;
import com.erp.order.model.InventoryMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    @Query("SELECT l FROM InventoryLog l WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(l.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(l.productSku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(COALESCE(l.referenceCode, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:movementType IS NULL OR l.movementType = :movementType) AND " +
           "(:fromDate IS NULL OR l.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR l.createdAt <= :toDate)")
    Page<InventoryLog> searchLogs(
            @Param("keyword") String keyword,
            @Param("movementType") InventoryMovementType movementType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    List<InventoryLog> findTop200ByOrderByCreatedAtDesc();
}
