package com.erp.scheduler.repository;

import com.erp.scheduler.model.JobExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Long> {
    Page<JobExecutionLog> findByJobCodeOrderByStartTimeDesc(String jobCode, Pageable pageable);
    Page<JobExecutionLog> findAllByOrderByStartTimeDesc(Pageable pageable);
    List<JobExecutionLog> findTop20ByJobCodeOrderByStartTimeDesc(String jobCode);
}

