package com.erp.branch.repository;

import com.erp.branch.model.InternalTransfer;
import com.erp.branch.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternalTransferRepository extends JpaRepository<InternalTransfer, Long> {

    Optional<InternalTransfer> findByTransferCode(String transferCode);

    boolean existsByTransferCode(String transferCode);

    List<InternalTransfer> findAllByOrderByCreatedAtDesc();

    List<InternalTransfer> findByStatusOrderByCreatedAtDesc(TransferStatus status);

    @Query("SELECT t FROM InternalTransfer t WHERE t.fromWarehouseId = :whId OR t.toWarehouseId = :whId ORDER BY t.createdAt DESC")
    List<InternalTransfer> findByWarehouseId(@Param("whId") Long whId);
}
