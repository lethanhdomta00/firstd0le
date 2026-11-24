package com.erp.branch.repository;

import com.erp.branch.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByCode(String code);
    boolean existsByCode(String code);
    List<Branch> findAllByOrderByCreatedAtDesc();
    List<Branch> findByStatus(String status);
}
