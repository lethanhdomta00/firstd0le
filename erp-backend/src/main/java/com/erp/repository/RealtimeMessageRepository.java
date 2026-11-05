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

    // Phân trang lazy loading: lấy tin nhắn mới nhất của phòng (id giảm dần)
    List<RealtimeMessage> findByRoomIdOrderByIdDesc(String roomId, Pageable pageable);

    // Phân trang lazy loading: lấy tin nhắn cũ hơn một id cụ thể
    List<RealtimeMessage> findByRoomIdAndIdLessThanOrderByIdDesc(String roomId, Long beforeId, Pageable pageable);

    // Lấy tất cả tin nhắn giữa 2 người (chat 1-1)
    @Query("SELECT m FROM RealtimeMessage m WHERE (m.senderUsername = :user1 AND m.recipientUsername = :user2) " +
           "OR (m.senderUsername = :user2 AND m.recipientUsername = :user1) ORDER BY m.createdAt ASC")
    List<RealtimeMessage> findPrivateMessagesBetween(@Param("user1") String user1, @Param("user2") String user2);

    // Lấy tin nhắn mới nhất giữa 2 người (chat 1-1)
    @Query("SELECT m FROM RealtimeMessage m WHERE (m.senderUsername = :user1 AND m.recipientUsername = :user2) " +
           "OR (m.senderUsername = :user2 AND m.recipientUsername = :user1) ORDER BY m.id DESC")
    List<RealtimeMessage> findLastMessageBetween(@Param("user1") String user1, @Param("user2") String user2, Pageable pageable);

    // Đếm số lượng tin nhắn chưa đọc gửi tới một người
    long countByRecipientUsernameAndStatus(String recipientUsername, MessageStatus status);

    // Đếm số lượng tin nhắn chưa đọc từ một người gửi cụ thể tới tôi
    @Query("SELECT COUNT(m) FROM RealtimeMessage m WHERE LOWER(m.senderUsername) = LOWER(:senderUsername) AND LOWER(m.recipientUsername) = LOWER(:recipientUsername) AND m.status = :status")
    long countUnreadFromSender(@Param("senderUsername") String senderUsername, 
                               @Param("recipientUsername") String recipientUsername, 
                               @Param("status") MessageStatus status);

    // Đếm số lượng tin nhắn chưa đọc trong 1 phòng cụ thể gửi tới một người
    long countByRoomIdAndRecipientUsernameAndStatus(String roomId, String recipientUsername, MessageStatus status);

    // Phân trang tin nhắn riêng 1-1 (mới nhất)
    @Query("SELECT m FROM RealtimeMessage m WHERE (LOWER(m.roomId) = LOWER(:roomId) OR " +
           "(LOWER(m.senderUsername) = LOWER(:u1) AND LOWER(m.recipientUsername) = LOWER(:u2)) OR " +
           "(LOWER(m.senderUsername) = LOWER(:u2) AND LOWER(m.recipientUsername) = LOWER(:u1))) " +
           "ORDER BY m.id DESC")
    List<RealtimeMessage> findPrivateHistoryLatest(@Param("roomId") String roomId, 
                                                   @Param("u1") String u1, 
                                                   @Param("u2") String u2, 
                                                   Pageable pageable);

    // Phân trang tin nhắn riêng 1-1 cũ hơn một ID (Lazy loading kéo lên trên)
    @Query("SELECT m FROM RealtimeMessage m WHERE ((LOWER(m.roomId) = LOWER(:roomId)) OR " +
           "(LOWER(m.senderUsername) = LOWER(:u1) AND LOWER(m.recipientUsername) = LOWER(:u2)) OR " +
           "(LOWER(m.senderUsername) = LOWER(:u2) AND LOWER(m.recipientUsername) = LOWER(:u1))) " +
           "AND m.id < :beforeId ORDER BY m.id DESC")
    List<RealtimeMessage> findPrivateHistoryBeforeId(@Param("roomId") String roomId, 
                                                     @Param("u1") String u1, 
                                                     @Param("u2") String u2, 
                                                     @Param("beforeId") Long beforeId, 
                                                     Pageable pageable);

    // Lấy danh sách tin nhắn được ghim trong phòng
    List<RealtimeMessage> findByRoomIdAndIsPinnedTrueOrderByPinnedAtDesc(String roomId);

    // Lấy tin nhắn được ghim giữa 2 người (chat 1-1)
    @Query("SELECT m FROM RealtimeMessage m WHERE ((m.roomId = :roomId) OR " +
           "(m.senderUsername = :user1 AND m.recipientUsername = :user2) OR " +
           "(m.senderUsername = :user2 AND m.recipientUsername = :user1)) AND m.isPinned = true ORDER BY m.pinnedAt DESC")
    List<RealtimeMessage> findPinnedMessagesBetween(@Param("roomId") String roomId, 
                                                    @Param("user1") String user1, 
                                                    @Param("user2") String user2);

    // Đánh dấu tất cả tin nhắn trong phòng gửi tới một người là ĐÃ ĐỌC (READ)
    @Modifying
    @Query("UPDATE RealtimeMessage m SET m.status = :readStatus WHERE m.roomId = :roomId AND m.recipientUsername = :recipientUsername AND m.status != :readStatus")
    int markMessagesAsReadInRoom(@Param("roomId") String roomId, 
                                 @Param("recipientUsername") String recipientUsername, 
                                 @Param("readStatus") MessageStatus readStatus);

    // Đánh dấu đã đọc tin nhắn trong phòng hoặc theo người gửi (Hỗ trợ chat 1-1 phòng riêng)
    @Modifying
    @Query("UPDATE RealtimeMessage m SET m.status = :readStatus " +
           "WHERE (LOWER(m.roomId) = LOWER(:roomId) OR (LOWER(m.recipientUsername) = LOWER(:recipientUsername) AND LOWER(m.senderUsername) = LOWER(:partnerUsername))) " +
           "AND LOWER(m.recipientUsername) = LOWER(:recipientUsername) AND m.status != :readStatus")
    int markMessagesAsReadInRoomWithPartner(@Param("roomId") String roomId,
                                            @Param("recipientUsername") String recipientUsername,
                                            @Param("partnerUsername") String partnerUsername,
                                            @Param("readStatus") MessageStatus readStatus);

    // Đánh dấu TẤT CẢ tin nhắn gửi tới một người là ĐÃ ĐỌC
    @Modifying
    @Query("UPDATE RealtimeMessage m SET m.status = :readStatus WHERE LOWER(m.recipientUsername) = LOWER(:recipientUsername) AND m.status != :readStatus")
    int markAllMessagesAsReadForUser(@Param("recipientUsername") String recipientUsername, 
                                     @Param("readStatus") MessageStatus readStatus);
}

