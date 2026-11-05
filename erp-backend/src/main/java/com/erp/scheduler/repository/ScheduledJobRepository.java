package com.erp.scheduler.repository;

import com.erp.scheduler.model.JobStatus;
import com.erp.scheduler.model.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
    Optional<ScheduledJob> findByJobCode(String jobCode);
    List<ScheduledJob> findByStatus(JobStatus status);
    boolean existsByJobCode(String jobCode);
    List<ScheduledJob> findAllByOrderByCreatedAtDesc();
}

