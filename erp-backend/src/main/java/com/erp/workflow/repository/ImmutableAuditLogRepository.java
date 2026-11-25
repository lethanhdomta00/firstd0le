package com.erp.workflow.repository;

import com.erp.workflow.model.ImmutableAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImmutableAuditLogRepository extends JpaRepository<ImmutableAuditLog, Long> {

    List<ImmutableAuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);

    @Query("SELECT a FROM ImmutableAuditLog a WHERE " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
            "(:toDate IS NULL OR a.timestamp <= :toDate) " +
            "ORDER BY a.timestamp DESC")
    Page<ImmutableAuditLog> searchAuditLogs(@Param("entityType") String entityType,
                                            @Param("username") String username,
                                            @Param("action") String action,
                                            @Param("fromDate") LocalDateTime fromDate,
                                            @Param("toDate") LocalDateTime toDate,
                                            Pageable pageable);
}
