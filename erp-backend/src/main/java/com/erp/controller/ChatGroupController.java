package com.erp.controller;

import com.erp.dto.AddMembersRequest;
import com.erp.dto.ChatGroupDto;
import com.erp.dto.ChatGroupMemberDto;
import com.erp.dto.CreateGroupRequest;
import com.erp.dto.RealtimeMessageDto;
import com.erp.model.ChatGroup;
import com.erp.model.ChatGroupMember;
import com.erp.model.MessageStatus;
import com.erp.model.MessageType;
import com.erp.model.RealtimeMessage;
import com.erp.model.User;
import com.erp.repository.ChatGroupMemberRepository;
import com.erp.repository.ChatGroupRepository;
import com.erp.repository.RealtimeMessageRepository;
import com.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/realtime-chat/groups")
@RequiredArgsConstructor
@Slf4j
public class ChatGroupController {

    private final ChatGroupRepository groupRepository;
    private final ChatGroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final RealtimeMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 1. Lấy danh sách nhóm chat mà tôi đang tham gia
     */
    @GetMapping("/my-groups")
    public ResponseEntity<List<ChatGroupDto>> getMyGroups() {
        String currentUsername = getCurrentUsername();
        List<ChatGroup> groups = groupRepository.findGroupsByUsername(currentUsername);

        List<ChatGroupDto> dtoList = groups.stream()
                .map(this::convertToDto)
                .sorted((a, b) -> {
                    java.time.LocalDateTime timeA = a.getLastMessageTime() != null ? a.getLastMessageTime() : a.getCreatedAt();
                    java.time.LocalDateTime timeB = b.getLastMessageTime() != null ? b.getLastMessageTime() : b.getCreatedAt();
                    if (timeA == null && timeB == null) return 0;
                    if (timeA == null) return 1;
                    if (timeB == null) return -1;
                    return timeB.compareTo(timeA);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * 2. Tạo nhóm chat mới
     */
    @PostMapping
    @Transactional
    public ResponseEntity<ChatGroupDto> createGroup(@RequestBody CreateGroupRequest request) {
        String currentUsername = getCurrentUsername();

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên nhóm không được để trống!");
        }

        // Tạo Entity Nhóm
        ChatGroup group = ChatGroup.builder()
                .name(request.getName().trim())
                .creatorUsername(currentUsername)
                .build();
        ChatGroup savedGroup = groupRepository.save(group);

        // Thêm người tạo làm ADMIN
        ChatGroupMember creatorMember = ChatGroupMember.builder()
                .groupId(savedGroup.getId())
                .username(currentUsername)
                .role("ADMIN")
                .build();
        memberRepository.save(creatorMember);

        // Thêm các thành viên được mời (nếu có)
        Set<String> invitedUsers = new HashSet<>();
        if (request.getMemberUsernames() != null) {
            for (String u : request.getMemberUsernames()) {
                if (u != null && !u.trim().isEmpty() && !u.equalsIgnoreCase(currentUsername)) {
                    invitedUsers.add(u.trim());
                }
            }
        }

        for (String invited : invitedUsers) {
            ChatGroupMember member = ChatGroupMember.builder()
                    .groupId(savedGroup.getId())
                    .username(invited)
                    .role("MEMBER")
                    .build();
            memberRepository.save(member);
        }

        // Gửi tin nhắn hệ thống chào mừng nhóm
        String roomId = "GROUP_" + savedGroup.getId();
        Optional<User> creatorUser = userRepository.findByUsername(currentUsername);
        String creatorName = creatorUser.map(User::getFullName).orElse(currentUsername);

        RealtimeMessage systemMsg = RealtimeMessage.builder()
                .roomId(roomId)
                .senderUsername("system")
                .senderFullName("Hệ thống")
                .content(creatorName + " đã tạo nhóm \"" + savedGroup.getName() + "\"")
                .type(MessageType.JOIN)
                .status(MessageStatus.SENT)
                .build();
        RealtimeMessage savedMsg = messageRepository.save(systemMsg);

        // Phát STOMP tới phòng nhóm
        messagingTemplate.convertAndSend("/topic/room." + roomId, RealtimeMessageDto.fromEntity(savedMsg));

        log.info("User [{}] đã tạo nhóm chat [{}] (id: {}) với {} thành viên", 
                currentUsername, savedGroup.getName(), savedGroup.getId(), invitedUsers.size() + 1);

        return ResponseEntity.ok(convertToDto(savedGroup));
    }

    /**
     * 3. Lấy chi tiết thông tin và danh sách thành viên của nhóm
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<ChatGroupDto> getGroupDetails(@PathVariable Long groupId) {
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm chat không tồn tại!"));

        String currentUsername = getCurrentUsername();
        if (!memberRepository.existsByGroupIdAndUsername(groupId, currentUsername)) {
            throw new RuntimeException("Bạn không phải thành viên của nhóm chat này!");
        }

        return ResponseEntity.ok(convertToDto(group));
    }

    /**
     * 4. Thêm thành viên vào nhóm chat
     */
    @PostMapping("/{groupId}/members")
    @Transactional
    public ResponseEntity<ChatGroupDto> addMembers(
            @PathVariable Long groupId,
            @RequestBody AddMembersRequest request) {

        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm chat không tồn tại!"));

        String currentUsername = getCurrentUsername();
        if (!memberRepository.existsByGroupIdAndUsername(groupId, currentUsername)) {
            throw new RuntimeException("Bạn không có quyền thêm thành viên vào nhóm này!");
        }

        List<String> addedNames = new ArrayList<>();
        if (request.getUsernames() != null) {
            for (String uname : request.getUsernames()) {
                if (uname == null || uname.trim().isEmpty()) continue;
                String clean = uname.trim();
                if (!memberRepository.existsByGroupIdAndUsername(groupId, clean)) {
                    ChatGroupMember newMember = ChatGroupMember.builder()
                            .groupId(groupId)
                            .username(clean)
                            .role("MEMBER")
                            .build();
                    memberRepository.save(newMember);

                    Optional<User> uOpt = userRepository.findByUsername(clean);
                    addedNames.add(uOpt.map(User::getFullName).orElse(clean));
                }
            }
        }

        if (!addedNames.isEmpty()) {
            String roomId = "GROUP_" + groupId;
            Optional<User> curUser = userRepository.findByUsername(currentUsername);
            String actorName = curUser.map(User::getFullName).orElse(currentUsername);

            RealtimeMessage sysMsg = RealtimeMessage.builder()
                    .roomId(roomId)
                    .senderUsername("system")
                    .senderFullName("Hệ thống")
                    .content(actorName + " đã thêm " + String.join(", ", addedNames) + " vào nhóm")
                    .type(MessageType.JOIN)
                    .status(MessageStatus.SENT)
                    .build();
            RealtimeMessage savedMsg = messageRepository.save(sysMsg);
            messagingTemplate.convertAndSend("/topic/room." + roomId, RealtimeMessageDto.fromEntity(savedMsg));
        }

        return ResponseEntity.ok(convertToDto(group));
    }

    /**
     * 5. Xóa thành viên khỏi nhóm chat (chỉ ADMIN / Trưởng nhóm mới có quyền)
     */
    @DeleteMapping("/{groupId}/members/{targetUsername}")
    @Transactional
    public ResponseEntity<?> removeMember(
            @PathVariable Long groupId,
            @PathVariable String targetUsername) {

        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm chat không tồn tại!"));

        String currentUsername = getCurrentUsername();
        ChatGroupMember myMembership = memberRepository.findByGroupIdAndUsername(groupId, currentUsername)
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc nhóm này!"));

        if (!"ADMIN".equalsIgnoreCase(myMembership.getRole()) && !group.getCreatorUsername().equalsIgnoreCase(currentUsername)) {
            throw new RuntimeException("Chỉ trưởng nhóm mới có quyền xóa thành viên!");
        }

        if (targetUsername.equalsIgnoreCase(group.getCreatorUsername())) {
            throw new RuntimeException("Không thể xóa trưởng nhóm khỏi nhóm chat!");
        }

        memberRepository.deleteByGroupIdAndUsername(groupId, targetUsername);

        Optional<User> targetUser = userRepository.findByUsername(targetUsername);
        String targetFullName = targetUser.map(User::getFullName).orElse(targetUsername);
        Optional<User> actorUser = userRepository.findByUsername(currentUsername);
        String actorFullName = actorUser.map(User::getFullName).orElse(currentUsername);

        String roomId = "GROUP_" + groupId;
        RealtimeMessage sysMsg = RealtimeMessage.builder()
                .roomId(roomId)
                .senderUsername("system")
                .senderFullName("Hệ thống")
                .content(actorFullName + " đã xóa " + targetFullName + " khỏi nhóm")
                .type(MessageType.LEAVE)
                .status(MessageStatus.SENT)
                .build();
        RealtimeMessage savedMsg = messageRepository.save(sysMsg);
        messagingTemplate.convertAndSend("/topic/room." + roomId, RealtimeMessageDto.fromEntity(savedMsg));

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đã xóa " + targetFullName + " khỏi nhóm thành công"
        ));
    }

    /**
     * 6. Tự rời nhóm chat
     */
    @PostMapping("/{groupId}/leave")
    @Transactional
    public ResponseEntity<?> leaveGroup(@PathVariable Long groupId) {
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm chat không tồn tại!"));

        String currentUsername = getCurrentUsername();
        if (!memberRepository.existsByGroupIdAndUsername(groupId, currentUsername)) {
            throw new RuntimeException("Bạn không thuộc nhóm này!");
        }

        // Xóa thành viên
        memberRepository.deleteByGroupIdAndUsername(groupId, currentUsername);

        // Kiểm tra xem nhóm còn ai không
        List<ChatGroupMember> remaining = memberRepository.findByGroupId(groupId);
        if (remaining.isEmpty()) {
            // Không còn ai: Xóa luôn nhóm
            groupRepository.delete(group);
            log.info("Nhóm [{}] đã bị xóa do tất cả thành viên đã rời nhóm", group.getName());
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Bạn đã rời nhóm. Nhóm đã bị giải tán."));
        }

        // Nếu người rời là trưởng nhóm: chuyển quyền ADMIN cho thành viên tiếp theo
        if (group.getCreatorUsername().equalsIgnoreCase(currentUsername)) {
            ChatGroupMember nextAdmin = remaining.get(0);
            nextAdmin.setRole("ADMIN");
            memberRepository.save(nextAdmin);
            group.setCreatorUsername(nextAdmin.getUsername());
            groupRepository.save(group);
            log.info("Chuyển quyền trưởng nhóm [{}] sang cho [{}]", group.getName(), nextAdmin.getUsername());
        }

        Optional<User> leaverUser = userRepository.findByUsername(currentUsername);
        String leaverName = leaverUser.map(User::getFullName).orElse(currentUsername);

        String roomId = "GROUP_" + groupId;
        RealtimeMessage sysMsg = RealtimeMessage.builder()
                .roomId(roomId)
                .senderUsername("system")
                .senderFullName("Hệ thống")
                .content(leaverName + " đã rời khỏi nhóm chat")
                .type(MessageType.LEAVE)
                .status(MessageStatus.SENT)
                .build();
        RealtimeMessage savedMsg = messageRepository.save(sysMsg);
        messagingTemplate.convertAndSend("/topic/room." + roomId, RealtimeMessageDto.fromEntity(savedMsg));

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Bạn đã rời nhóm thành công"
        ));
    }

    // =========================================================================
    //                            HELPER METHODS
    // =========================================================================

    private ChatGroupDto convertToDto(ChatGroup group) {
        List<ChatGroupMember> members = memberRepository.findByGroupId(group.getId());

        // Lấy thông tin họ tên & avatar của các thành viên
        List<String> usernames = members.stream().map(ChatGroupMember::getUsername).collect(Collectors.toList());
        Map<String, User> userMap = userRepository.findAll().stream()
                .filter(u -> usernames.contains(u.getUsername()))
                .collect(Collectors.toMap(User::getUsername, u -> u));

        List<ChatGroupMemberDto> memberDtos = members.stream()
                .map(m -> {
                    User u = userMap.get(m.getUsername());
                    return ChatGroupMemberDto.builder()
                            .id(m.getId())
                            .groupId(m.getGroupId())
                            .username(m.getUsername())
                            .fullName(u != null ? u.getFullName() : m.getUsername())
                            .avatarUrl(u != null ? u.getAvatarUrl() : null)
                            .role(m.getRole())
                            .joinedAt(m.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());

        User creator = userMap.get(group.getCreatorUsername());

        String roomId = "GROUP_" + group.getId();
        List<RealtimeMessage> lastMsgs = messageRepository.findByRoomIdOrderByIdDesc(roomId, PageRequest.of(0, 1));
        String lastMessageContent = null;
        java.time.LocalDateTime lastMessageTime = null;
        String lastMessageStatus = null;
        String lastMessageSender = null;
        if (!lastMsgs.isEmpty()) {
            RealtimeMessage m = lastMsgs.get(0);
            if (Boolean.TRUE.equals(m.getIsDeleted())) {
                lastMessageContent = "Tin nhắn đã được thu hồi";
            } else if (m.getType() == MessageType.IMAGE) {
                lastMessageContent = "📷 [Hình ảnh]";
            } else if (m.getType() == MessageType.VIDEO) {
                lastMessageContent = "🎥 [Video]";
            } else {
                lastMessageContent = m.getContent();
            }
            lastMessageTime = m.getCreatedAt();
            lastMessageStatus = m.getStatus() != null ? m.getStatus().name() : "SENT";
            lastMessageSender = m.getSenderUsername();
        }

        return ChatGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .creatorUsername(group.getCreatorUsername())
                .creatorFullName(creator != null ? creator.getFullName() : group.getCreatorUsername())
                .avatarUrl(group.getAvatarUrl())
                .memberCount(members.size())
                .members(memberDtos)
                .lastMessageContent(lastMessageContent)
                .lastMessageTime(lastMessageTime)
                .lastMessageStatus(lastMessageStatus)
                .lastMessageSender(lastMessageSender)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Chưa đăng nhập!");
        }
        return auth.getName();
    }
}

