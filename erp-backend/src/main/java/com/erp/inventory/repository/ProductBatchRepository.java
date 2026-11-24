package com.erp.inventory.repository;

import com.erp.inventory.model.BatchStatus;
import com.erp.inventory.model.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    Optional<ProductBatch> findByBatchCode(String batchCode);

    boolean existsByBatchCode(String batchCode);

    List<ProductBatch> findAllByOrderByCreatedAtDesc();

    List<ProductBatch> findByWarehouseIdOrderByExpiryDateAsc(Long warehouseId);

    List<ProductBatch> findByProductId(Long productId);

    /**
     * Thuật toán FEFO (First Expired, First Out)
     * Lấy các lô còn hàng của sản phẩm, ưu tiên lô có hạn dùng gần nhất trước
     */
    @Query("SELECT b FROM ProductBatch b WHERE b.productId = :productId AND b.remainingQuantity > :minQty AND b.status <> 'EXPIRED' ORDER BY b.expiryDate ASC, b.id ASC")
    List<ProductBatch> findBatchesForFefo(@Param("productId") Long productId, @Param("minQty") Integer minQty);

    /**
     * Thuật toán FIFO (First In, First Out)
     * Lấy các lô còn hàng của sản phẩm, ưu tiên lô nhập kho trước
     */
    @Query("SELECT b FROM ProductBatch b WHERE b.productId = :productId AND b.remainingQuantity > :minQty ORDER BY b.createdAt ASC, b.id ASC")
    List<ProductBatch> findBatchesForFifo(@Param("productId") Long productId, @Param("minQty") Integer minQty);

    /**
     * Tìm các lô hàng cận date (Hạn dùng nằm trong khoảng từ today đến targetDate)
     */
    @Query("SELECT b FROM ProductBatch b WHERE b.remainingQuantity > 0 AND b.expiryDate BETWEEN :today AND :targetDate ORDER BY b.expiryDate ASC")
    List<ProductBatch> findNearExpiryBatches(@Param("today") LocalDate today, @Param("targetDate") LocalDate targetDate);

    /**
     * Tìm các lô hàng đã quá hạn sử dụng
     */
    @Query("SELECT b FROM ProductBatch b WHERE b.remainingQuantity > 0 AND b.expiryDate < :today ORDER BY b.expiryDate ASC")
    List<ProductBatch> findExpiredBatches(@Param("today") LocalDate today);

    long countByStatus(BatchStatus status);
}
