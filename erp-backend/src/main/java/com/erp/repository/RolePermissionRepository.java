package com.erp.repository;

import com.erp.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleName(String roleName);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.roleName = :roleName")
    void deleteByRoleName(@Param("roleName") String roleName);

    boolean existsByRoleName(String roleName);
}
