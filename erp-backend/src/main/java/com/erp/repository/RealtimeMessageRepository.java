package com.erp.repository;

import com.erp.model.MessageStatus;
import com.erp.model.RealtimeMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RealtimeMessageRepository extends JpaRepository<RealtimeMessage, Long> {

    // Lấy tin nhắn theo phòng theo thứ tự thời gian tăng dần
    List<RealtimeMessage> findByRoomIdOrderByCreatedAtAsc(String roomId);

    // Lấy tin nhắn mới nhất có phân trang
    List<RealtimeMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    // Lấy tất cả tin nhắn giữa 2 người (chat 1-1)
    @Query("SELECT m FROM RealtimeMessage m WHERE (m.senderUsername = :user1 AND m.recipientUsername = :user2) " +
           "OR (m.senderUsername = :user2 AND m.recipientUsername = :user1) ORDER BY m.createdAt ASC")
    List<RealtimeMessage> findPrivateMessagesBetween(@Param("user1") String user1, @Param("user2") String user2);

    // Đếm số lượng tin nhắn chưa đọc gửi tới một người
    long countByRecipientUsernameAndStatus(String recipientUsername, MessageStatus status);

    // Đếm số lượng tin nhắn chưa đọc trong 1 phòng cụ thể gửi tới một người
    long countByRoomIdAndRecipientUsernameAndStatus(String roomId, String recipientUsername, MessageStatus status);

    // Đánh dấu tất cả tin nhắn trong phòng gửi tới một người là ĐÃ ĐỌC (READ)
    @Modifying
    @Query("UPDATE RealtimeMessage m SET m.status = :readStatus WHERE m.roomId = :roomId AND m.recipientUsername = :recipientUsername AND m.status != :readStatus")
    int markMessagesAsReadInRoom(@Param("roomId") String roomId, 
                                 @Param("recipientUsername") String recipientUsername, 
                                 @Param("readStatus") MessageStatus readStatus);
}

