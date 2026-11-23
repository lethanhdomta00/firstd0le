package com.erp.promotion.repository;

import com.erp.promotion.model.PromotionVoucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionVoucherRepository extends JpaRepository<PromotionVoucher, Long> {

    Optional<PromotionVoucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT v FROM PromotionVoucher v WHERE " +
           "(:keyword IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:active IS NULL OR v.active = :active)")
    Page<PromotionVoucher> searchVouchers(@Param("keyword") String keyword, 
                                          @Param("active") Boolean active, 
                                          Pageable pageable);

    @Query("SELECT v FROM PromotionVoucher v WHERE v.active = true AND " +
           "(v.startDate IS NULL OR v.startDate <= :now) AND " +
           "(v.endDate IS NULL OR v.endDate >= :now)")
    List<PromotionVoucher> findActiveVouchers(@Param("now") LocalDateTime now);

    long countByActiveTrue();
}
