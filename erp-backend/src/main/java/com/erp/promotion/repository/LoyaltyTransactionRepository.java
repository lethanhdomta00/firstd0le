package com.erp.promotion.repository;

import com.erp.promotion.model.LoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    Page<LoyaltyTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    List<LoyaltyTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
