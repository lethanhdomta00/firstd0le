package com.erp.finance.repository;

import com.erp.finance.model.CostCenter;
import com.erp.finance.model.FinanceVoucher;
import com.erp.finance.model.VoucherStatus;
import com.erp.finance.model.VoucherType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinanceVoucherRepository extends JpaRepository<FinanceVoucher, Long> {

    Optional<FinanceVoucher> findByVoucherCode(String voucherCode);

    @Query("SELECT v FROM FinanceVoucher v WHERE " +
            "(:voucherType IS NULL OR v.voucherType = :voucherType) AND " +
            "(:status IS NULL OR v.status = :status) AND " +
            "(:costCenter IS NULL OR v.costCenter = :costCenter) AND " +
            "(:sourceAccountId IS NULL OR v.sourceAccount.id = :sourceAccountId) AND " +
            "(:targetAccountId IS NULL OR v.targetAccount.id = :targetAccountId) AND " +
            "(:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(v.payeePayorName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(v.referenceOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(v.note) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:startDate IS NULL OR v.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR v.createdAt <= :endDate) " +
            "ORDER BY v.createdAt DESC")
    Page<FinanceVoucher> searchVouchers(
            @Param("voucherType") VoucherType voucherType,
            @Param("status") VoucherStatus status,
            @Param("costCenter") CostCenter costCenter,
            @Param("sourceAccountId") Long sourceAccountId,
            @Param("targetAccountId") Long targetAccountId,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // SQL Aggregate: Tổng thu APPROVED trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(v.amount), 0) FROM FinanceVoucher v " +
            "WHERE v.voucherType = 'RECEIPT' AND v.status = 'APPROVED' " +
            "AND v.createdAt >= :startDate AND v.createdAt <= :endDate")
    BigDecimal sumTotalReceipts(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // SQL Aggregate: Tổng chi APPROVED trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(v.amount), 0) FROM FinanceVoucher v " +
            "WHERE v.voucherType = 'PAYMENT' AND v.status = 'APPROVED' " +
            "AND v.createdAt >= :startDate AND v.createdAt <= :endDate")
    BigDecimal sumTotalPayments(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // SQL Aggregate: Chi phí theo từng Trung tâm chi phí (Cost Center Breakdown)
    @Query("SELECT v.costCenter, COALESCE(SUM(v.amount), 0), COUNT(v) FROM FinanceVoucher v " +
            "WHERE v.voucherType = 'PAYMENT' AND v.status = 'APPROVED' " +
            "AND v.createdAt >= :startDate AND v.createdAt <= :endDate " +
            "GROUP BY v.costCenter ORDER BY SUM(v.amount) DESC")
    List<Object[]> aggregateExpensesByCostCenter(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // SQL Aggregate: Chi phí bình quân 30 ngày qua để tính Burn Rate (Tốc độ đốt tiền)
    @Query("SELECT COALESCE(SUM(v.amount), 0) FROM FinanceVoucher v " +
            "WHERE v.voucherType = 'PAYMENT' AND v.status = 'APPROVED' " +
            "AND v.createdAt >= :sinceDate")
    BigDecimal sumExpensesSince(@Param("sinceDate") LocalDateTime sinceDate);

    List<FinanceVoucher> findTop10ByStatusOrderByCreatedAtDesc(VoucherStatus status);
}
