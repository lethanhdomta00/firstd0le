package com.erp.branch.repository;

import com.erp.branch.model.InternalTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalTransferItemRepository extends JpaRepository<InternalTransferItem, Long> {
    List<InternalTransferItem> findByTransferId(Long transferId);
}
