package com.erp.procurement.repository;

import com.erp.procurement.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByCode(String code);

    Optional<Supplier> findByPhone(String phone);

    boolean existsByCode(String code);

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR s.phone LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:status IS NULL OR s.status = :status)")
    Page<Supplier> searchSuppliers(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    List<Supplier> findTop10ByOrderByCurrentDebtDesc();

    @Query("SELECT COALESCE(SUM(s.currentDebt), 0) FROM Supplier s")
    BigDecimal sumTotalAccountsPayable();

    long countByStatus(String status);
}
