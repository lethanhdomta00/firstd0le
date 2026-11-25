package com.erp.workflow.repository;

import com.erp.workflow.model.ApprovalDocumentType;
import com.erp.workflow.model.ApprovalRequest;
import com.erp.workflow.model.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    Optional<ApprovalRequest> findByRequestCode(String requestCode);

    Optional<ApprovalRequest> findByOneTimeToken(String oneTimeToken);

    Optional<ApprovalRequest> findFirstByDocumentTypeAndDocumentIdOrderByCreatedAtDesc(ApprovalDocumentType documentType, Long documentId);

    Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable);

    @Query("SELECT a FROM ApprovalRequest a WHERE " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:docType IS NULL OR a.documentType = :docType) AND " +
            "(:search IS NULL OR LOWER(a.requestCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.documentCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ApprovalRequest> searchApprovals(@Param("status") ApprovalStatus status,
                                          @Param("docType") ApprovalDocumentType docType,
                                          @Param("search") String search,
                                          Pageable pageable);

    long countByStatus(ApprovalStatus status);

    @Query("SELECT a.status, COUNT(a) FROM ApprovalRequest a GROUP BY a.status")
    List<Object[]> countGroupByStatus();
}
