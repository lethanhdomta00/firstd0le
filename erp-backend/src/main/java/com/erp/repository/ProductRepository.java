package com.erp.repository;

import com.erp.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    // Native FOR UPDATE works across MariaDB and MySQL (avoids Hibernate MySQLDialect "FOR UPDATE OF alias" syntax error)
    @Query(value = "SELECT * FROM products WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:stockStatus IS NULL OR :stockStatus = '' OR :stockStatus = 'ALL' OR " +
           " (:stockStatus = 'IN_STOCK' AND p.stock > 10) OR " +
           " (:stockStatus = 'LOW_STOCK' AND p.stock > 0 AND p.stock <= 10) OR " +
           " (:stockStatus = 'OUT_OF_STOCK' AND p.stock = 0))")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("stockStatus") String stockStatus,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.stock <= :threshold ORDER BY p.stock ASC")
    java.util.List<Product> findTopLowStockProducts(@Param("threshold") int threshold, Pageable pageable);

    /**
     * 1. Giữ chỗ tồn kho nguyên tử (Atomic Stock Reservation - Chống bán lố Race Condition):
     * Tăng reserved_stock và giảm available stock ngay lập tức.
     * Trả về số dòng cập nhật: = 1 nếu thành công, = 0 nếu tồn khả dụng không đủ!
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET " +
           "p.stock = (COALESCE(p.physicalStock, 0) - (COALESCE(p.reservedStock, 0) + :qty)), " +
           "p.reservedStock = COALESCE(p.reservedStock, 0) + :qty " +
           "WHERE p.id = :productId AND (COALESCE(p.physicalStock, 0) - COALESCE(p.reservedStock, 0)) >= :qty")
    int reserveStockAtomic(@Param("productId") Long productId, @Param("qty") int qty);

    /**
     * 2. Giải phóng giữ chỗ tồn kho (Release Reservation - Khi Hủy đơn PENDING):
     * Giảm reserved_stock và hoàn lại available stock.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET " +
           "p.stock = (COALESCE(p.physicalStock, 0) - CASE WHEN COALESCE(p.reservedStock, 0) >= :qty THEN p.reservedStock - :qty ELSE 0 END), " +
           "p.reservedStock = CASE WHEN COALESCE(p.reservedStock, 0) >= :qty THEN p.reservedStock - :qty ELSE 0 END " +
           "WHERE p.id = :productId")
    int releaseReservedStock(@Param("productId") Long productId, @Param("qty") int qty);

    /**
     * 3. Xuất kho thực tế (Deduct Physical Stock - Khi đơn CONFIRMED/SHIPPING):
     * Giảm cả physical_stock trên kệ và reserved_stock.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET " +
           "p.stock = (CASE WHEN COALESCE(p.physicalStock, 0) >= :qty THEN p.physicalStock - :qty ELSE 0 END - " +
           "           CASE WHEN COALESCE(p.reservedStock, 0) >= :qty THEN p.reservedStock - :qty ELSE 0 END), " +
           "p.physicalStock = CASE WHEN COALESCE(p.physicalStock, 0) >= :qty THEN p.physicalStock - :qty ELSE 0 END, " +
           "p.reservedStock = CASE WHEN COALESCE(p.reservedStock, 0) >= :qty THEN p.reservedStock - :qty ELSE 0 END " +
           "WHERE p.id = :productId")
    int deductPhysicalStock(@Param("productId") Long productId, @Param("qty") int qty);

    /**
     * 4. Hoàn kho vật lý (Restore Physical Stock - Khi khách trả hàng đã xuất kho):
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET " +
           "p.stock = ((COALESCE(p.physicalStock, 0) + :qty) - COALESCE(p.reservedStock, 0)), " +
           "p.physicalStock = COALESCE(p.physicalStock, 0) + :qty " +
           "WHERE p.id = :productId")
    int restorePhysicalStock(@Param("productId") Long productId, @Param("qty") int qty);
}
