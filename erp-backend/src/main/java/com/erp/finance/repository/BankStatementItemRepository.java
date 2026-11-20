package com.erp.finance.repository;

import com.erp.finance.model.BankStatementItem;
import com.erp.finance.model.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankStatementItemRepository extends JpaRepository<BankStatementItem, Long> {

    Optional<BankStatementItem> findByTransactionCode(String transactionCode);

    List<BankStatementItem> findByStatusOrderByTransactionDateDesc(ReconciliationStatus status);

    @Query("SELECT b FROM BankStatementItem b WHERE " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:keyword IS NULL OR LOWER(b.transactionCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(b.matchedOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(b.matchedCustomerName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY b.transactionDate DESC")
    Page<BankStatementItem> searchStatementItems(
            @Param("status") ReconciliationStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT COUNT(b) FROM BankStatementItem b WHERE b.status = 'MATCHED'")
    long countMatchedItems();

    @Query("SELECT COUNT(b) FROM BankStatementItem b WHERE b.status = 'PENDING'")
    long countPendingItems();

    @Query("SELECT COUNT(b) FROM BankStatementItem b WHERE b.status = 'RECONCILED'")
    long countReconciledItems();
}
