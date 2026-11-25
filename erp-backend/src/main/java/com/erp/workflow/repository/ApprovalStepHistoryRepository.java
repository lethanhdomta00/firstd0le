package com.erp.workflow.repository;

import com.erp.workflow.model.ApprovalStepHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalStepHistoryRepository extends JpaRepository<ApprovalStepHistory, Long> {

    List<ApprovalStepHistory> findByApprovalRequestIdOrderByStepLevelAscActionTimeAsc(Long approvalRequestId);
}
