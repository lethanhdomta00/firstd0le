package com.erp.repository;

import com.erp.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
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
}
