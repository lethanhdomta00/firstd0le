package com.erp.workflow.repository;

import com.erp.workflow.model.ApprovalDocumentType;
import com.erp.workflow.model.ApprovalMatrixRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ApprovalMatrixRuleRepository extends JpaRepository<ApprovalMatrixRule, Long> {

    List<ApprovalMatrixRule> findByDocumentTypeAndActiveTrueOrderByPriorityAscMinAmountAscLevelAsc(ApprovalDocumentType documentType);

    List<ApprovalMatrixRule> findByActiveTrueOrderByDocumentTypeAscPriorityAscLevelAsc();

    @Query("SELECT r FROM ApprovalMatrixRule r " +
            "WHERE r.documentType = :docType " +
            "AND r.active = true " +
            "AND r.minAmount <= :amount " +
            "AND (r.maxAmount IS NULL OR r.maxAmount >= :amount) " +
            "ORDER BY r.level ASC")
    List<ApprovalMatrixRule> findMatchingRules(@Param("docType") ApprovalDocumentType docType,
                                               @Param("amount") BigDecimal amount);
}
