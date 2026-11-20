package com.erp.finance.repository;

import com.erp.finance.model.TreasuryAccount;
import com.erp.finance.model.TreasuryAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TreasuryAccountRepository extends JpaRepository<TreasuryAccount, Long> {

    Optional<TreasuryAccount> findByAccountCode(String accountCode);

    List<TreasuryAccount> findByActiveTrueOrderByAccountTypeAscAccountNameAsc();

    List<TreasuryAccount> findByAccountTypeAndActiveTrue(TreasuryAccountType accountType);

    @Query("SELECT COALESCE(SUM(t.currentBalance), 0) FROM TreasuryAccount t WHERE t.active = true")
    BigDecimal getTotalBalance();

    @Query("SELECT COALESCE(SUM(t.currentBalance), 0) FROM TreasuryAccount t WHERE t.active = true AND t.accountType = 'CASH'")
    BigDecimal getTotalCashBalance();

    @Query("SELECT COALESCE(SUM(t.currentBalance), 0) FROM TreasuryAccount t WHERE t.active = true AND t.accountType = 'BANK'")
    BigDecimal getTotalBankBalance();
}
