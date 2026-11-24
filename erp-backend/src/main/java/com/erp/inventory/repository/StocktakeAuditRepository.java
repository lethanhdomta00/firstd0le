package com.erp.inventory.repository;

import com.erp.inventory.model.StocktakeAudit;
import com.erp.inventory.model.StocktakeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StocktakeAuditRepository extends JpaRepository<StocktakeAudit, Long> {

    Optional<StocktakeAudit> findByAuditCode(String auditCode);

    boolean existsByAuditCode(String auditCode);

    List<StocktakeAudit> findAllByOrderByCreatedAtDesc();

    List<StocktakeAudit> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    List<StocktakeAudit> findByStatusOrderByCreatedAtDesc(StocktakeStatus status);
}
