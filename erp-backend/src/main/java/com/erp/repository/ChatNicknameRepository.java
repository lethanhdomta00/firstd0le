package com.erp.repository;

import com.erp.model.ChatNickname;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatNicknameRepository extends JpaRepository<ChatNickname, Long> {
    List<ChatNickname> findByRoomId(String roomId);
    Optional<ChatNickname> findByRoomIdAndUsername(String roomId, String username);
}

