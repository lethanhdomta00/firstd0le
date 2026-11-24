package com.erp.branch.repository;

import com.erp.branch.model.WarehouseStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {

    Optional<WarehouseStock> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<WarehouseStock> findByProductId(Long productId);

    List<WarehouseStock> findByWarehouseId(Long warehouseId);

    List<WarehouseStock> findByBranchId(Long branchId);

    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.productId IN :productIds")
    List<WarehouseStock> findByProductIdIn(@Param("productIds") List<Long> productIds);

    @Query("SELECT COALESCE(SUM(ws.availableStock), 0) FROM WarehouseStock ws WHERE ws.productId = :productId")
    Integer sumAvailableStockByProductId(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(ws.physicalStock), 0) FROM WarehouseStock ws WHERE ws.productId = :productId")
    Integer sumPhysicalStockByProductId(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(ws.inTransitStock), 0) FROM WarehouseStock ws WHERE ws.productId = :productId")
    Integer sumInTransitStockByProductId(@Param("productId") Long productId);

    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.availableStock <= ws.minStockAlert ORDER BY ws.availableStock ASC")
    List<WarehouseStock> findLowStockAlerts();
}
