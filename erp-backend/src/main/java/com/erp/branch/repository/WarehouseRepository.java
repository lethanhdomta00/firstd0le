package com.erp.branch.repository;

import com.erp.branch.model.Warehouse;
import com.erp.branch.model.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);
    boolean existsByCode(String code);
    List<Warehouse> findByBranchId(Long branchId);
    List<Warehouse> findByWarehouseType(WarehouseType warehouseType);
    List<Warehouse> findByActiveTrue();
    List<Warehouse> findAllByOrderByCreatedAtDesc();
}
