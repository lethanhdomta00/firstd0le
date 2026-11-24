package com.erp.inventory.repository;

import com.erp.inventory.model.ProductSerial;
import com.erp.inventory.model.SerialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSerialRepository extends JpaRepository<ProductSerial, Long> {

    Optional<ProductSerial> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumber(String serialNumber);

    List<ProductSerial> findAllByOrderByCreatedAtDesc();

    List<ProductSerial> findByProductId(Long productId);

    List<ProductSerial> findByWarehouseId(Long warehouseId);

    List<ProductSerial> findByStatus(SerialStatus status);

    List<ProductSerial> findByOrderId(Long orderId);

    List<ProductSerial> findByCustomerPhone(String customerPhone);

    @Query("SELECT s FROM ProductSerial s WHERE LOWER(s.serialNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.productName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.productSku) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.customerPhone) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ProductSerial> searchSerials(@Param("query") String query);
}
