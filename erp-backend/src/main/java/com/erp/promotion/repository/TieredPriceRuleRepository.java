package com.erp.promotion.repository;

import com.erp.promotion.model.TieredPriceRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TieredPriceRuleRepository extends JpaRepository<TieredPriceRule, Long> {

    @Query("SELECT r FROM TieredPriceRule r LEFT JOIN FETCH r.product WHERE r.active = true AND " +
           "(r.product.id = :productId OR r.product IS NULL) ORDER BY r.minQuantity DESC")
    List<TieredPriceRule> findActiveRulesForProduct(@Param("productId") Long productId);

    @Query("SELECT r FROM TieredPriceRule r LEFT JOIN FETCH r.product ORDER BY r.createdAt DESC")
    Page<TieredPriceRule> findAllWithProduct(Pageable pageable);

    List<TieredPriceRule> findByProductIdAndActiveTrue(Long productId);
}
