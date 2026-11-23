package com.erp.promotion.repository;

import com.erp.promotion.model.VoucherUsageHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface VoucherUsageHistoryRepository extends JpaRepository<VoucherUsageHistory, Long> {

    long countByVoucherCodeIgnoreCase(String voucherCode);

    long countByVoucherCodeIgnoreCaseAndCustomerId(String voucherCode, Long customerId);

    Page<VoucherUsageHistory> findByVoucherCodeIgnoreCaseOrderByUsedAtDesc(String voucherCode, Pageable pageable);

    Page<VoucherUsageHistory> findByCustomerIdOrderByUsedAtDesc(Long customerId, Pageable pageable);

    Optional<VoucherUsageHistory> findByOrderId(Long orderId);

    @Query("SELECT COALESCE(SUM(h.discountApplied), 0) FROM VoucherUsageHistory h")
    BigDecimal sumTotalDiscountSponsored();
}
