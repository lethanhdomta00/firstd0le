package com.erp.repository;

import com.erp.model.ChatGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, Long> {

    List<ChatGroupMember> findByGroupId(Long groupId);

    List<ChatGroupMember> findByUsername(String username);

    Optional<ChatGroupMember> findByGroupIdAndUsername(Long groupId, String username);

    boolean existsByGroupIdAndUsername(Long groupId, String username);

    @Modifying
    void deleteByGroupIdAndUsername(Long groupId, String username);

    @Modifying
    void deleteByGroupId(Long groupId);

    long countByGroupId(Long groupId);
}

