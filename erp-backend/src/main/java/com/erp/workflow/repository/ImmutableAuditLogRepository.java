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

    @Query("SELECT a FROM ImmutableAuditLog a WHERE " +
            "(:keyword IS NULL OR " +
            " LOWER(a.traceId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(a.entityCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(a.reason) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
            "(:toDate IS NULL OR a.timestamp <= :toDate) " +
            "ORDER BY a.timestamp DESC")
    Page<ImmutableAuditLog> searchAuditLogsAdvanced(@Param("keyword") String keyword,
                                                    @Param("entityType") String entityType,
                                                    @Param("username") String username,
                                                    @Param("action") String action,
                                                    @Param("fromDate") LocalDateTime fromDate,
                                                    @Param("toDate") LocalDateTime toDate,
                                                    Pageable pageable);

    @Query("SELECT COUNT(a) FROM ImmutableAuditLog a WHERE a.timestamp >= :since")
    long countLogsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM ImmutableAuditLog a WHERE a.timestamp >= :since AND a.action IN :actions")
    long countRiskyLogsSince(@Param("since") LocalDateTime since, @Param("actions") List<String> actions);

    @Query("SELECT COUNT(DISTINCT a.username) FROM ImmutableAuditLog a WHERE a.timestamp >= :since")
    long countDistinctUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.entityType, COUNT(a) FROM ImmutableAuditLog a WHERE a.timestamp >= :since GROUP BY a.entityType ORDER BY COUNT(a) DESC")
    List<Object[]> countByEntityTypeSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.action, COUNT(a) FROM ImmutableAuditLog a WHERE a.timestamp >= :since GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByActionSince(@Param("since") LocalDateTime since);
}
