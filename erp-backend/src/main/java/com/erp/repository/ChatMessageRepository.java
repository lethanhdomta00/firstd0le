package com.erp.repository;

import com.erp.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Truy vấn lịch sử hội thoại RIÊNG BIỆT theo ID của tài khoản đang đăng nhập
    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);

    // Xóa sạch lịch sử hội thoại RIÊNG BIỆT của tài khoản đang đăng nhập
    @Transactional
    @Modifying
    void deleteByUserId(Long userId);
}

