package com.erp.repository;

import com.erp.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Tăng dư nợ khi lên đơn mua hàng nợ
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Customer c SET c.currentDebt = COALESCE(c.currentDebt, 0) + :amount WHERE c.id = :customerId")
    int increaseDebt(@Param("customerId") Long customerId, @Param("amount") BigDecimal amount);

    /**
     * Trừ dư nợ khi khách thanh toán gạch nợ (VietQR hoặc tiền mặt)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Customer c SET c.currentDebt = CASE WHEN COALESCE(c.currentDebt, 0) >= :amount THEN c.currentDebt - :amount ELSE 0 END WHERE c.id = :customerId")
    int decreaseDebt(@Param("customerId") Long customerId, @Param("amount") BigDecimal amount);

    /**
     * Tìm danh sách khách hàng có dư nợ để xuất Báo cáo Tuổi nợ (Aging Debt)
     */
    java.util.List<Customer> findByCurrentDebtGreaterThanOrderByCurrentDebtDesc(BigDecimal amount);
}
