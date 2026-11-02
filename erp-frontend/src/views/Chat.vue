<template>
  <div class="realtime-chat-page">
    <!-- KHUNG CHAT TỔNG HỢP (SPLIT LAYOUT) -->
    <div class="chat-card-container">
      
      <!-- CỘT TRÁI: DANH BẠ KÊNH & ĐỒNG NGHIỆP -->
      <aside class="chat-sidebar">
        <!-- Header danh bạ -->
        <div class="sidebar-header">
          <div class="sidebar-header-top">
            <div class="header-title-row">
              <div class="chat-brand-icon">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <h2 class="sidebar-title">Trò chuyện nội bộ</h2>
            </div>
            
            <!-- Trạng thái kết nối WebSocket -->
            <div class="ws-status-badge" :class="connectionStatusClass" :title="connectionTooltip">
              <span class="status-pulse"></span>
              <span class="status-text">{{ connectionStatusText }}</span>
            </div>
          </div>

          <!-- Ô tìm kiếm đồng nghiệp -->
          <div class="search-box">
            <svg class="search-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input 
              v-model="searchKeyword" 
              type="text" 
              placeholder="Tìm kiếm kênh hoặc đồng nghiệp..." 
              class="search-input"
            />
            <button v-if="searchKeyword" class="search-clear-btn" @click="searchKeyword = ''">✕</button>
          </div>
        </div>

        <!-- Danh sách Kênh & Hội thoại -->
        <div class="directory-list-scroll">
          
          <!-- PHẦN 1: KÊNH CHUNG TOÀN DOANH NGHIỆP -->
          <div class="section-label">KÊNH TỔ CHỨC</div>
          <div 
            class="directory-item"
            :class="{ 'is-active': chatStore.activeChatType === 'PUBLIC' }"
            @click="selectPublicRoom"
          >
            <div class="item-avatar channel-avatar">
              <span class="hash-tag">#</span>
            </div>
            <div class="item-info">
              <div class="item-top-row">
                <span class="item-name">Phòng chung công ty</span>
                <span class="item-tag-public">Toàn thể</span>
              </div>
              <div v-if="isPublicTyping" class="sidebar-typing-indicator">
                <span class="sidebar-typing-text">Có người đang nhập</span>
                <span class="typing-dots-mini">
                  <span></span><span></span><span></span>
                </span>
              </div>
              <p v-else class="item-preview">Thảo luận công việc & thông báo nhanh</p>
            </div>
            <span v-if="chatStore.unreadCounts['GLOBAL']" class="unread-pill">
              {{ chatStore.unreadCounts['GLOBAL'] }}
            </span>
          </div>

          <!-- PHẦN 2: NHÓM CHAT (GROUP CHAT) -->
          <div class="section-label-row">
            <span class="section-label">NHÓM CHAT ({{ chatStore.myGroups.length }})</span>
            <button class="btn-create-group" title="Tạo nhóm chat mới" @click="openCreateGroupDialog">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
            </button>
          </div>

          <div v-if="chatStore.myGroups.length === 0" class="empty-groups-hint">
            <span>Chưa tham gia nhóm nào</span>
            <button type="button" class="btn-link-create" @click="openCreateGroupDialog">+ Tạo nhóm ngay</button>
          </div>

          <div 
            v-for="grp in chatStore.myGroups" 
            :key="grp.id"
            class="directory-item"
            :class="{ 'is-active': chatStore.activeChatType === 'GROUP' && chatStore.activeGroup?.id === grp.id }"
            @click="selectGroupRoom(grp)"
          >
            <div class="item-avatar group-avatar-box" :style="getAvatarBg(grp.name)">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
            </div>
            <div class="item-info">
              <div class="item-top-row">
                <span class="item-name">{{ grp.name }}</span>
                <span v-if="grp.lastMessageTime" class="item-time">{{ formatSidebarTime(grp.lastMessageTime) }}</span>
                <span v-else class="item-tag-group">{{ grp.memberCount }} tv</span>
              </div>
              <div class="item-preview-row">
                <div v-if="isGroupTyping(grp.id)" class="sidebar-typing-indicator">
                  <span class="sidebar-typing-text">{{ getGroupTypingNames(grp.id) }} đang nhập</span>
                  <span class="typing-dots-mini">
                    <span></span><span></span><span></span>
                  </span>
                </div>
                <template v-else>
                  <span 
                    v-if="grp.lastMessageSender === authStore.user?.username" 
                    class="sidebar-status-icon"
                    :class="grp.lastMessageStatus === 'READ' ? 'status-read' : 'status-sent'"
                    :title="grp.lastMessageStatus === 'READ' ? 'Đã xem' : 'Đã gửi'"
                  >
                    <svg v-if="grp.lastMessageStatus === 'READ'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#2563eb" stroke-width="2.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M2 13l4 4L16 7M8 13l4 4L22 7" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#94a3b8" stroke-width="2.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <p class="item-preview" :class="{ 'is-unread-bold': chatStore.unreadCounts['GROUP_' + grp.id] }">
                    {{ grp.lastMessageContent || `${grp.memberCount} thành viên` }}
                  </p>
                </template>
              </div>
            </div>
            <span v-if="chatStore.unreadCounts['GROUP_' + grp.id]" class="unread-pill">
              {{ chatStore.unreadCounts['GROUP_' + grp.id] }}
            </span>
            <span 
              v-else-if="grp.lastMessageSender === authStore.user?.username && grp.lastMessageStatus === 'READ'" 
              class="seen-pill"
              title="Thành viên đã xem"
            >
              Đã xem
            </span>
          </div>

          <!-- PHẦN 3: TIN NHẮN RIÊNG 1-1 (ĐỒNG NGHIỆP) -->
          <div class="section-label-row">
            <span class="section-label">ĐỒNG NGHIỆP ({{ filteredColleagues.length }})</span>
            <button class="btn-refresh-colleagues" title="Làm mới danh sách" @click="chatStore.fetchColleagues">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          </div>

          <div v-if="filteredColleagues.length === 0" class="empty-colleagues">
            <p>Không tìm thấy đồng nghiệp phù hợp</p>
          </div>

          <div 
            v-for="colleague in filteredColleagues" 
            :key="colleague.id"
            class="directory-item"
            :class="{ 'is-active': chatStore.activeChatType === 'PRIVATE' && chatStore.activePartner?.username === colleague.username }"
            @click="selectPrivatePartner(colleague)"
          >
            <div class="item-avatar-wrapper">
              <img 
                v-if="colleague.avatarUrl" 
                :src="colleague.avatarUrl" 
                class="item-avatar" 
                alt="avatar" 
              />
              <div v-else class="item-avatar default-avatar" :style="getAvatarBg(colleague.fullName || colleague.username)">
                {{ getAvatarInitials(colleague.fullName || colleague.username) }}
              </div>
              <span class="online-indicator-dot"></span>
            </div>

            <div class="item-info">
              <div class="item-top-row">
                <span class="item-name">{{ chatStore.getNickname(colleague.username, colleague.fullName || colleague.username) }}</span>
                <span v-if="colleague.lastMessageTime" class="item-time">{{ formatSidebarTime(colleague.lastMessageTime) }}</span>
                <span v-else class="role-badge" :class="colleague.role === 'ADMIN' ? 'role-admin' : 'role-user'">
                  {{ colleague.role === 'ADMIN' ? 'ADMIN' : 'STAFF' }}
                </span>
              </div>
              <div class="item-preview-row">
                <div v-if="isColleagueTyping(colleague.username)" class="sidebar-typing-indicator">
                  <span class="sidebar-typing-text">đang nhập</span>
                  <span class="typing-dots-mini">
                    <span></span><span></span><span></span>
                  </span>
                </div>
                <template v-else>
                  <span 
                    v-if="colleague.lastMessageSender === authStore.user?.username" 
                    class="sidebar-status-icon"
                    :class="colleague.lastMessageStatus === 'READ' ? 'status-read' : 'status-sent'"
                    :title="colleague.lastMessageStatus === 'READ' ? 'Đã xem' : 'Đã gửi'"
                  >
                    <svg v-if="colleague.lastMessageStatus === 'READ'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#2563eb" stroke-width="2.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M2 13l4 4L16 7M8 13l4 4L22 7" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#94a3b8" stroke-width="2.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <p class="item-preview" :class="{ 'is-unread-bold': getPrivateUnreadCount(colleague.username) }">
                    {{ colleague.lastMessageContent || `@${colleague.username}` }}
                  </p>
                </template>
              </div>
            </div>

            <span 
              v-if="getPrivateUnreadCount(colleague.username)" 
              class="unread-pill"
            >
              {{ getPrivateUnreadCount(colleague.username) }}
            </span>
            <span 
              v-else-if="colleague.lastMessageSender === authStore.user?.username && colleague.lastMessageStatus === 'READ'" 
              class="seen-pill"
              title="Đồng nghiệp đã xem tin nhắn"
            >
              Đã xem
            </span>
          </div>

        </div>
      </aside>

      <!-- CỘT PHẢI: KHUNG HỘI THOẠI TRỰC TUYẾN CHÍNH -->
      <main class="chat-main">
        
        <!-- Header khung hội thoại -->
        <header class="main-header">
          <div class="current-room-info">
            <!-- Icon/Avatar phòng đang mở -->
            <div v-if="chatStore.activeChatType === 'PUBLIC'" class="header-room-avatar channel-avatar">
              <span class="hash-tag">#</span>
            </div>
            <div v-else-if="chatStore.activeChatType === 'GROUP'" class="header-room-avatar group-avatar-box" :style="getAvatarBg(activeTitle)">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
            </div>
            <div v-else class="header-room-avatar-wrapper">
              <img 
                v-if="chatStore.activePartner?.avatarUrl" 
                :src="chatStore.activePartner.avatarUrl" 
                class="header-room-avatar" 
              />
              <div v-else class="header-room-avatar default-avatar" :style="getAvatarBg(activeTitle)">
                {{ getAvatarInitials(activeTitle) }}
              </div>
              <span class="online-indicator-dot"></span>
            </div>

            <div class="header-title-group">
              <div class="title-with-badge">
                <h3 class="current-chat-name">{{ activeTitle }}</h3>
                <span v-if="chatStore.activeChatType === 'PUBLIC'" class="badge-tag-channel">Phòng chung</span>
                <span v-else-if="chatStore.activeChatType === 'GROUP'" class="badge-tag-group">Nhóm ({{ chatStore.activeGroup?.memberCount }} tv)</span>
              </div>
              <p class="current-chat-desc" :class="{ 'is-typing-desc': currentRoomIsTyping }">
                <span v-if="currentRoomIsTyping" class="typing-dot-blink">●</span>
                {{ activeSubtitle }}
              </p>
            </div>
          </div>

          <!-- Nút hành động nhanh trên Header -->
          <div class="header-actions">
            <!-- Nút Quản lý Thành viên Nhóm -->
            <button 
              v-if="chatStore.activeChatType === 'GROUP'"
              class="btn-group-manage-action"
              title="Xem và quản lý thành viên nhóm"
              @click="openGroupMembersModal"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
              <span>Thành viên ({{ chatStore.activeGroup?.memberCount }})</span>
            </button>

            <!-- Nút Đặt Biệt danh cho cuộc trò chuyện -->
            <button 
              v-if="chatStore.activeChatType !== 'PUBLIC'"
              class="btn-icon-action" 
              title="Đặt biệt danh trong cuộc trò chuyện"
              @click="openNicknameModal"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
                <line x1="7" y1="7" x2="7.01" y2="7"></line>
              </svg>
            </button>

            <!-- Nút Bật/Tắt âm thanh thông báo -->
            <button 
              class="btn-icon-action" 
              :class="{ 'is-muted': !chatStore.isSoundEnabled }"
              :title="chatStore.isSoundEnabled ? 'Âm thanh thông báo: BẬT (Bấm để tắt)' : 'Âm thanh thông báo: TẮT (Bấm để bật)'"
              @click="handleToggleSound"
            >
              <svg v-if="chatStore.isSoundEnabled" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                <line x1="23" y1="9" x2="17" y2="15"></line>
                <line x1="17" y1="9" x2="23" y2="15"></line>
              </svg>
            </button>

            <!-- Nút Bật thông báo Desktop màn hình -->
            <button 
              class="btn-icon-action" 
              :class="{ 'is-notif-active': chatStore.isNotificationEnabled }"
              :title="chatStore.isNotificationEnabled ? 'Thông báo Desktop: ĐANG BẬT' : 'Bật thông báo Desktop khi rời tab'"
              @click="handleToggleNotification"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
              </svg>
              <span v-if="chatStore.isNotificationEnabled" class="notif-active-dot"></span>
            </button>

            <button 
              class="btn-icon-action" 
              title="Tải lại lịch sử tin nhắn"
              @click="refreshCurrentHistory"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          </div>
        </header>

        <!-- Vùng hiển thị tin nhắn (Scroll container) -->
        <div ref="messagesScrollRef" class="messages-viewport" @scroll="handleScroll">
          
          <!-- Loading tin nhắn cũ hơn (Lazy Loading kéo lên đỉnh) -->
          <div v-if="chatStore.isLoadingOlder" class="lazy-loading-spinner-row">
            <span class="lazy-spinner-icon"></span>
            <span>Đang tải tin nhắn cũ hơn...</span>
          </div>
          <div v-else-if="chatStore.hasMoreHistory[chatStore.activeRoomId] === false && chatStore.currentMessages.length > 15" class="lazy-end-banner">
            <span>Đã xem toàn bộ lịch sử trò chuyện</span>
          </div>

          <!-- Thông báo chào mừng đầu phòng -->
          <div class="room-welcome-banner">
            <div class="welcome-icon-box">
              <svg v-if="chatStore.activeChatType === 'PUBLIC'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              <svg v-else-if="chatStore.activeChatType === 'GROUP'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </div>
            <h4 class="welcome-title">
              {{ chatStore.activeChatType === 'PUBLIC' 
                ? 'Chào mừng đến Kênh chung Doanh nghiệp!' 
                : (chatStore.activeChatType === 'GROUP' ? `Nhóm chat: ${activeTitle}` : `Cuộc trò chuyện với ${activeTitle}`) }}
            </h4>
            <p class="welcome-desc">
              {{ chatStore.activeChatType === 'PUBLIC' 
                ? 'Đây là không gian trao đổi, phối hợp công việc thời gian thực giữa các phòng ban.' 
                : (chatStore.activeChatType === 'GROUP' 
                  ? `Nhóm được tạo bởi ${chatStore.activeGroup?.creatorFullName || chatStore.activeGroup?.creatorUsername}. Mọi thành viên trong nhóm đều nhận tin nhắn tức thì.` 
                  : 'Tin nhắn riêng tư giữa hai đồng nghiệp được đồng bộ thời gian thực qua giao thức WebSocket STOMP.') }}
            </p>
          </div>

          <!-- Danh sách bong bóng tin nhắn -->
          <div 
            v-for="(msg, index) in chatStore.currentMessages" 
            :key="msg.id || index"
            :id="msg.id ? `msg-${msg.id}` : undefined"
            class="message-bubble-row"
            :class="[isMyMessage(msg) ? 'is-outgoing' : 'is-incoming', { 'is-pending': msg.status === 'SENDING' }]"
          >
            <!-- Avatar người gửi (chỉ hiển thị với tin nhắn nhận được) -->
            <div v-if="!isMyMessage(msg)" class="sender-avatar-col">
              <img 
                v-if="msg.senderAvatarUrl" 
                :src="msg.senderAvatarUrl" 
                class="bubble-avatar" 
                alt="avatar" 
              />
              <div v-else class="bubble-avatar default-avatar" :style="getAvatarBg(msg.senderFullName || msg.senderUsername)">
                {{ getAvatarInitials(msg.senderFullName || msg.senderUsername) }}
              </div>
            </div>

            <!-- Nội dung tin nhắn -->
            <div class="bubble-content-col">
              <!-- Tên người gửi (khi trong phòng chung hoặc nhóm) -->
              <div v-if="!isMyMessage(msg) && (chatStore.activeChatType === 'PUBLIC' || chatStore.activeChatType === 'GROUP')" class="sender-name-row">
                <span class="sender-name">{{ chatStore.getNickname(msg.senderUsername, msg.senderFullName || msg.senderUsername) }}</span>
                <span class="sender-username">@{{ msg.senderUsername }}</span>
              </div>

              <!-- Cụm Bong bóng và Thanh hành động nhanh (Hover Actions) -->
              <div class="bubble-and-actions">
                <!-- Bong bóng chứa nội dung tin nhắn -->
                <div 
                  class="message-bubble" 
                  :class="{ 
                    'is-image-bubble': msg.type === 'IMAGE', 
                    'is-video-bubble': msg.type === 'VIDEO', 
                    'is-deleted-bubble': msg.isDeleted 
                  }"
                >
                  
                  <!-- Trường hợp: Tin nhắn ĐÃ BỊ THU HỒI / XÓA -->
                  <div v-if="msg.isDeleted" class="bubble-deleted-box">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"></circle>
                      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                    </svg>
                    <span class="deleted-msg-text">Tin nhắn đã được thu hồi</span>
                  </div>

                  <template v-else>
                    <!-- Khối hiển thị tin nhắn được Reply (nếu có) -->
                    <div 
                      v-if="msg.replyToId" 
                      class="bubble-reply-quote"
                      title="Bấm để cuộn đến tin nhắn gốc"
                      @click="scrollToMessage(msg.replyToId)"
                    >
                      <div class="quote-bar"></div>
                      <div class="quote-text-group">
                        <span class="quote-sender">↩ {{ chatStore.getNickname(msg.replyToSender, msg.replyToSender) || 'Đồng nghiệp' }}</span>
                        <span class="quote-snippet">{{ msg.replyToContent }}</span>
                      </div>
                    </div>

                    <!-- Trường hợp: Tin nhắn hình ảnh -->
                    <div 
                      v-if="msg.type === 'IMAGE'" 
                      class="bubble-image-wrapper" 
                      title="Nhấn để phóng to hình ảnh"
                      @click="openImageLightbox(msg.content)"
                    >
                      <img :src="msg.content" class="bubble-image" alt="Ảnh chia sẻ" loading="lazy" />
                      <div class="image-zoom-overlay">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                          <circle cx="11" cy="11" r="8"></circle>
                          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                          <line x1="11" y1="8" x2="11" y2="14"></line>
                          <line x1="8" y1="11" x2="14" y2="11"></line>
                        </svg>
                        <span>Phóng to</span>
                      </div>
                    </div>

                    <!-- Trường hợp: Tin nhắn Video -->
                    <div v-else-if="msg.type === 'VIDEO'" class="bubble-video-wrapper">
                      <video :src="msg.content" controls playsinline class="bubble-video" preload="metadata"></video>
                    </div>

                    <!-- Trường hợp: Tin nhắn văn bản thông thường -->
                    <div v-else class="bubble-text">{{ msg.content }}</div>
                  </template>
                  
                  <!-- Thời gian & trạng thái gửi -->
                  <div class="bubble-meta">
                    <span v-if="msg.isEdited && !msg.isDeleted" class="meta-edited-tag" title="Tin nhắn đã chỉnh sửa">(đã chỉnh sửa)</span>
                    <span class="meta-time">{{ formatTime(msg.createdAt) }}</span>
                    <span v-if="isMyMessage(msg) && !msg.isDeleted" class="meta-status-check">
                      <span v-if="msg.status === 'SENDING'" class="status-sending-spinner" title="Đang gửi..."></span>
                      <!-- ĐÃ XEM: 2 dấu tích xanh sáng rõ + chữ Đã xem -->
                      <span v-else-if="msg.status === 'READ'" class="status-read-double-check" title="Đã xem">
                        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="#38bdf8" stroke-width="2.5">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M2 13l4 4L16 7M8 13l4 4L22 7" />
                        </svg>
                        <span class="read-status-label">Đã xem</span>
                      </span>
                      <!-- ĐÃ GỬI: 1 dấu tích -->
                      <span v-else class="status-sent-single-check" title="Đã gửi (Chưa xem)">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                      </span>
                    </span>
                  </div>

                  <!-- Danh sách Reaction đã thả trên tin nhắn -->
                  <div v-if="!msg.isDeleted && msg.reactions && Object.keys(msg.reactions).length > 0" class="bubble-reactions-row">
                    <button
                      v-for="(users, emoji) in msg.reactions"
                      :key="emoji"
                      type="button"
                      class="reaction-chip"
                      :class="{ 'is-my-reaction': users.includes(authStore.user?.username) }"
                      :title="users.join(', ')"
                      @click.stop="chatStore.toggleReaction(msg.id, emoji)"
                    >
                      <span class="reaction-emoji">{{ emoji }}</span>
                      <span class="reaction-count">{{ users.length }}</span>
                    </button>
                  </div>
                </div>

                <!-- Thanh thao tác nhanh khi Hover lên bong bóng (Reply, Reaction, Edit, Delete) -->
                <div v-if="!msg.isDeleted" class="message-actions-hover">
                  <button 
                    v-for="emoji in ['👍', '❤️', '😂', '🔥']" 
                    :key="emoji"
                    type="button" 
                    class="btn-hover-reaction" 
                    :title="`Thả ${emoji}`"
                    @click="chatStore.toggleReaction(msg.id, emoji)"
                  >
                    {{ emoji }}
                  </button>
                  <button 
                    type="button" 
                    class="btn-hover-reply" 
                    title="Trả lời tin nhắn này"
                    @click="handleStartReply(msg)"
                  >
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="9 14 4 9 9 4"></polyline>
                      <path d="M20 20v-7a4 4 0 0 0-4-4H4"></path>
                    </svg>
                  </button>
                  <!-- Nút Chỉnh sửa (chỉ tin nhắn văn bản của chính mình) -->
                  <button 
                    v-if="isMyMessage(msg) && msg.type === 'CHAT'" 
                    type="button" 
                    class="btn-hover-edit" 
                    title="Chỉnh sửa tin nhắn"
                    @click="handleStartEdit(msg)"
                  >
                    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>
                  <!-- Nút Thu hồi / Xóa (tin nhắn của mình hoặc Admin nhóm) -->
                  <button 
                    v-if="isMyMessage(msg) || isCurrentUserGroupAdmin" 
                    type="button" 
                    class="btn-hover-delete" 
                    title="Thu hồi tin nhắn"
                    @click="handleDeleteMessage(msg)"
                  >
                    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                  </button>
                </div>
              </div>

            </div>
          </div>

          <!-- Chỉ báo đang nhập tin nhắn (Typing Indicator) -->
          <div v-if="chatStore.currentTypingUsers.length > 0" class="typing-indicator-row">
            <div class="typing-bubble">
              <span class="typing-names">{{ chatStore.currentTypingUsers.join(', ') }} đang nhập</span>
              <span class="bouncing-dots">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
            </div>
          </div>

        </div>

        <!-- Nút cuộn nhanh xuống tin nhắn mới nhất -->
        <button 
          v-if="showScrollToBottom" 
          class="btn-scroll-bottom" 
          @click="scrollToBottom(true)"
          title="Cuộn xuống tin nhắn mới nhất"
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
          </svg>
        </button>

        <!-- Footer soạn thảo và gửi tin nhắn -->
        <footer class="main-footer">
          <!-- Gợi ý emoji nhanh -->
          <div class="quick-reactions-bar">
            <button 
              v-for="emoji in quickEmojis" 
              :key="emoji" 
              type="button" 
              class="btn-quick-emoji"
              @click="insertEmoji(emoji)"
            >
              {{ emoji }}
            </button>
          </div>

          <!-- Thanh xem trước tin nhắn đang Reply (nếu có) -->
          <div v-if="chatStore.replyingTo" class="reply-preview-bar">
            <div class="reply-preview-left">
              <div class="reply-preview-icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 14 4 9 9 4"></polyline>
                  <path d="M20 20v-7a4 4 0 0 0-4-4H4"></path>
                </svg>
              </div>
              <div class="reply-preview-text">
                <span class="reply-to-user">Đang trả lời <strong>{{ chatStore.getNickname(chatStore.replyingTo.senderUsername, chatStore.replyingTo.senderFullName || chatStore.replyingTo.senderUsername) }}</strong></span>
                <span class="reply-to-content">{{ chatStore.replyingTo.type === 'IMAGE' ? '📷 [Hình ảnh]' : (chatStore.replyingTo.type === 'VIDEO' ? '🎥 [Video]' : chatStore.replyingTo.content) }}</span>
              </div>
            </div>
            <button type="button" class="btn-cancel-reply" title="Hủy trả lời" @click="chatStore.cancelReply">✕</button>
          </div>

          <!-- Thanh hiển thị đang chỉnh sửa tin nhắn -->
          <div v-if="editingMessage" class="edit-preview-bar">
            <div class="edit-preview-left">
              <div class="edit-preview-icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
              </div>
              <div class="edit-preview-text">
                <span class="edit-to-user">Đang chỉnh sửa tin nhắn</span>
                <span class="edit-to-content">{{ editingMessage.content }}</span>
              </div>
            </div>
            <button type="button" class="btn-cancel-edit" title="Hủy chỉnh sửa (ESC)" @click="cancelEdit">✕</button>
          </div>

          <form class="chat-input-form" @submit.prevent="handleSendMessage">
            <!-- Input file ẩn để đính kèm hình ảnh hoặc video -->
            <input 
              ref="imageFileInputRef" 
              type="file" 
              accept="image/*,video/*" 
              class="hidden-file-input" 
              @change="handleMediaSelected"
            />

            <!-- Nút đính kèm ảnh hoặc video -->
            <button 
              type="button" 
              class="btn-attach-image" 
              :class="{ 'is-uploading': isUploadingImage }"
              :disabled="isUploadingImage || !chatStore.isConnected"
              :title="isUploadingImage ? 'Đang tải tệp lên Cloud...' : 'Đính kèm hình ảnh hoặc video (hoặc Ctrl+V)'"
              @click="triggerImageSelect"
            >
              <svg v-if="!isUploadingImage" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <circle cx="8.5" cy="8.5" r="1.5"></circle>
                <polyline points="21 15 16 10 5 21"></polyline>
              </svg>
              <span v-else class="upload-spinner-icon"></span>
            </button>

            <textarea
              ref="inputTextareaRef"
              v-model="inputContent"
              class="chat-textarea"
              :placeholder="editingMessage ? 'Nhập nội dung chỉnh sửa... (Nhấn Enter để lưu, ESC để hủy)' : 'Nhập nội dung tin nhắn hoặc dán ảnh/video (Ctrl+V)... (Enter gửi, Shift+Enter xuống dòng)'"
              rows="2"
              @keydown.enter.exact.prevent="handleSendMessage"
              @keydown.esc="cancelEdit"
              @input="handleTyping"
              @paste="handlePaste"
            ></textarea>

            <button 
              type="submit" 
              class="btn-send-message"
              :disabled="!inputContent.trim() || !chatStore.isConnected"
            >
              <span>{{ editingMessage ? 'Lưu' : 'Gửi' }}</span>
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            </button>
          </form>
        </footer>

      </main>
    </div>

    <!-- MODAL LIGHTBOX XEM ẢNH FULLSIZE -->
    <Teleport to="body">
      <div 
        v-if="previewImageUrl" 
        class="image-lightbox-overlay" 
        @click.self="previewImageUrl = null"
      >
        <div class="lightbox-dialog">
          <div class="lightbox-header">
            <span class="lightbox-title">Xem trước hình ảnh</span>
            <div class="lightbox-actions">
              <a 
                :href="previewImageUrl" 
                target="_blank" 
                rel="noopener noreferrer" 
                class="btn-lightbox-tool" 
                title="Mở ảnh gốc trong tab mới"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                  <polyline points="15 3 21 3 21 9"></polyline>
                  <line x1="10" y1="14" x2="21" y2="3"></line>
                </svg>
                <span>Ảnh gốc</span>
              </a>
              <button 
                type="button" 
                class="btn-lightbox-close" 
                title="Đóng (ESC)" 
                @click="previewImageUrl = null"
              >
                ✕
              </button>
            </div>
          </div>
          <div class="lightbox-body">
            <img :src="previewImageUrl" class="lightbox-img" alt="Xem ảnh lớn" />
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL TẠO NHÓM CHAT MỚI -->
    <Teleport to="body">
      <div v-if="showCreateGroupDialog" class="modal-backdrop-overlay" @click.self="showCreateGroupDialog = false">
        <div class="custom-modal-card">
          <div class="modal-card-header">
            <div class="header-icon-title">
              <div class="modal-header-icon">👥</div>
              <h3 class="modal-card-title">Tạo nhóm trò chuyện mới</h3>
            </div>
            <button type="button" class="btn-close-modal" @click="showCreateGroupDialog = false">✕</button>
          </div>

          <div class="modal-card-body">
            <div class="form-group-field">
              <label class="field-label">Tên nhóm chat <span class="required-star">*</span></label>
              <input 
                v-model="newGroupName" 
                type="text" 
                class="form-text-input" 
                placeholder="Ví dụ: Team Dự Án ERP, Phòng Marketing..."
                @keydown.enter.prevent="handleCreateGroup"
              />
            </div>

            <div class="form-group-field">
              <label class="field-label">Chọn đồng nghiệp tham gia ({{ selectedGroupMembers.length }} đã chọn)</label>
              <div class="colleagues-checkbox-scroll">
                <label 
                  v-for="colleague in chatStore.colleagues" 
                  :key="colleague.id" 
                  class="colleague-check-item"
                >
                  <input 
                    type="checkbox" 
                    :value="colleague.username" 
                    v-model="selectedGroupMembers" 
                    class="check-box-input"
                  />
                  <div class="check-avatar" :style="getAvatarBg(colleague.fullName || colleague.username)">
                    {{ getAvatarInitials(colleague.fullName || colleague.username) }}
                  </div>
                  <div class="check-info">
                    <span class="check-name">{{ colleague.fullName || colleague.username }}</span>
                    <span class="check-username">@{{ colleague.username }}</span>
                  </div>
                </label>
              </div>
            </div>
          </div>

          <div class="modal-card-footer">
            <button type="button" class="btn-modal-cancel" @click="showCreateGroupDialog = false">Hủy</button>
            <button 
              type="button" 
              class="btn-modal-primary" 
              :disabled="!newGroupName.trim() || isCreatingGroup"
              @click="handleCreateGroup"
            >
              <span v-if="isCreatingGroup" class="btn-spinner"></span>
              <span>{{ isCreatingGroup ? 'Đang tạo...' : 'Tạo nhóm chat' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL QUẢN LÝ THÀNH VIÊN NHÓM -->
    <Teleport to="body">
      <div v-if="showGroupMembersModal" class="modal-backdrop-overlay" @click.self="showGroupMembersModal = false">
        <div class="custom-modal-card modal-large">
          <div class="modal-card-header">
            <div class="header-icon-title">
              <div class="modal-header-icon">👥</div>
              <div class="modal-title-group">
                <h3 class="modal-card-title">{{ chatStore.activeGroup?.name }}</h3>
                <span class="modal-card-desc">{{ chatStore.activeGroup?.memberCount }} thành viên • Trưởng nhóm: {{ chatStore.activeGroup?.creatorFullName || chatStore.activeGroup?.creatorUsername }}</span>
              </div>
            </div>
            <button type="button" class="btn-close-modal" @click="showGroupMembersModal = false">✕</button>
          </div>

          <div class="modal-card-body">
            <!-- Thêm thành viên mới (nếu có đồng nghiệp chưa tham gia) -->
            <div v-if="availableColleaguesForGroup.length > 0" class="add-members-section">
              <div class="add-members-header">
                <span class="sub-section-title">Thêm thành viên mới</span>
              </div>
              <div class="add-members-form">
                <div class="colleagues-select-scroll">
                  <label 
                    v-for="colleague in availableColleaguesForGroup" 
                    :key="colleague.id" 
                    class="colleague-check-chip"
                  >
                    <input 
                      type="checkbox" 
                      :value="colleague.username" 
                      v-model="selectedNewMemberUsernames" 
                    />
                    <span>{{ colleague.fullName || colleague.username }}</span>
                  </label>
                </div>
                <button 
                  type="button" 
                  class="btn-submit-add-members" 
                  :disabled="!selectedNewMemberUsernames.length || isAddingMembers"
                  @click="handleAddMembersToGroup"
                >
                  {{ isAddingMembers ? 'Đang thêm...' : `+ Thêm vào nhóm (${selectedNewMemberUsernames.length})` }}
                </button>
              </div>
            </div>

            <!-- Danh sách thành viên hiện tại -->
            <div class="current-members-section">
              <span class="sub-section-title">Danh sách thành viên hiện tại ({{ chatStore.activeGroup?.members?.length }})</span>
              <div class="members-table-scroll">
                <div 
                  v-for="member in chatStore.activeGroup?.members" 
                  :key="member.id" 
                  class="member-row-item"
                >
                  <div class="member-left-col">
                    <img v-if="member.avatarUrl" :src="member.avatarUrl" class="member-avatar-img" />
                    <div v-else class="member-avatar-img default-avatar" :style="getAvatarBg(member.fullName || member.username)">
                      {{ getAvatarInitials(member.fullName || member.username) }}
                    </div>
                    <div class="member-info-col">
                      <div class="member-name-row">
                        <span class="member-name">{{ member.fullName || member.username }}</span>
                        <span 
                          class="member-role-badge"
                          :class="member.role === 'ADMIN' || member.username === chatStore.activeGroup?.creatorUsername ? 'badge-role-admin' : 'badge-role-member'"
                        >
                          {{ member.role === 'ADMIN' || member.username === chatStore.activeGroup?.creatorUsername ? 'TRƯỞNG NHÓM' : 'THÀNH VIÊN' }}
                        </span>
                      </div>
                      <span class="member-username">@{{ member.username }}</span>
                    </div>
                  </div>

                  <div class="member-right-actions">
                    <!-- Nút Xóa thành viên (chỉ Trưởng nhóm thấy và không thể xóa chính mình) -->
                    <button 
                      v-if="isCurrentUserGroupAdmin && member.username !== chatStore.activeGroup?.creatorUsername"
                      type="button" 
                      class="btn-remove-member"
                      title="Xóa thành viên khỏi nhóm"
                      @click="handleRemoveMember(member.username, member.fullName)"
                    >
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                      </svg>
                      <span>Xóa</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-card-footer modal-footer-between">
            <button type="button" class="btn-leave-group" @click="handleLeaveGroup">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
              </svg>
              <span>Rời khỏi nhóm</span>
            </button>
            <button type="button" class="btn-modal-cancel" @click="showGroupMembersModal = false">Đóng</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL ĐẶT BIỆT DANH (NICKNAMES) -->
    <Teleport to="body">
      <div v-if="showNicknameModal" class="modal-backdrop-overlay" @click.self="showNicknameModal = false">
        <div class="custom-modal-card">
          <div class="modal-card-header">
            <div class="header-icon-title">
              <div class="modal-header-icon">🏷️</div>
              <div class="modal-title-group">
                <h3 class="modal-card-title">Biệt danh cuộc trò chuyện</h3>
                <span class="modal-card-desc">Mỗi cuộc trò chuyện có thể cài đặt biệt danh riêng</span>
              </div>
            </div>
            <button type="button" class="btn-close-modal" @click="showNicknameModal = false">✕</button>
          </div>

          <div class="modal-card-body">
            <div class="nickname-list-scroll">
              <div 
                v-for="user in currentRoomMembersForNickname" 
                :key="user.username" 
                class="nickname-row-item"
              >
                <div class="nickname-user-left">
                  <div class="nickname-avatar" :style="getAvatarBg(user.fullName || user.username)">
                    {{ getAvatarInitials(user.fullName || user.username) }}
                  </div>
                  <div class="nickname-user-names">
                    <span class="nickname-user-fullname">
                      {{ user.fullName || user.username }}
                      <span v-if="user.username === authStore.user?.username" class="tag-me">(Bạn)</span>
                    </span>
                    <span class="nickname-user-sub">@{{ user.username }}</span>
                  </div>
                </div>

                <div class="nickname-input-box">
                  <input 
                    v-model="nicknameInputs[user.username]" 
                    type="text" 
                    class="form-text-input nickname-input"
                    :placeholder="`Biệt danh cho ${user.username}`"
                    maxlength="50"
                    @keydown.enter.prevent="handleSaveNickname(user.username)"
                  />
                  <button 
                    type="button" 
                    class="btn-save-nickname"
                    :disabled="isSavingNickname[user.username]"
                    @click="handleSaveNickname(user.username)"
                  >
                    {{ isSavingNickname[user.username] ? '...' : 'Lưu' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-card-footer">
            <button type="button" class="btn-modal-cancel" @click="showNicknameModal = false">Đóng</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { useRealtimeChatStore } from '../store/realtimeChat'

const authStore = useAuthStore()
const chatStore = useRealtimeChatStore()

// State nội bộ
const searchKeyword = ref('')
const inputContent = ref('')
const messagesScrollRef = ref(null)
const inputTextareaRef = ref(null)
const showScrollToBottom = ref(false)

// State đính kèm hình ảnh / video & Lightbox
const imageFileInputRef = ref(null)
const isUploadingImage = ref(false)
const previewImageUrl = ref(null)

// State Chỉnh sửa tin nhắn
const editingMessage = ref(null)

// State Biệt danh (Nicknames)
const showNicknameModal = ref(false)
const nicknameInputs = ref({})
const isSavingNickname = ref({})

// State Nhóm chat (Group Chat)
const showCreateGroupDialog = ref(false)
const showGroupMembersModal = ref(false)
const newGroupName = ref('')
const selectedGroupMembers = ref([])
const isCreatingGroup = ref(false)
const isAddingMembers = ref(false)
const selectedNewMemberUsernames = ref([])

// Quick emojis
const quickEmojis = ['👍', '👏', '🎉', '😊', '❤️', '🔥', '🚀', '✅']

// Trạng thái kết nối WebSocket STOMP
const connectionStatusClass = computed(() => {
  if (chatStore.isConnected) return 'status-online'
  if (chatStore.isConnecting) return 'status-connecting'
  return 'status-offline'
})

const connectionStatusText = computed(() => {
  if (chatStore.isConnected) return 'Trực tuyến'
  if (chatStore.isConnecting) return 'Đang kết nối...'
  return 'Mất kết nối'
})

const connectionTooltip = computed(() => {
  if (chatStore.isConnected) return 'Đã kết nối máy chủ thời gian thực (WebSocket STOMP)'
  if (chatStore.isConnecting) return 'Đang thiết lập kênh truyền WebSocket...'
  return 'Chưa kết nối. Hệ thống sẽ tự động thử lại sau vài giây.'
})

// Tiêu đề phòng đang mở
const activeTitle = computed(() => {
  if (chatStore.activeChatType === 'PUBLIC') {
    return 'Phòng chung toàn công ty'
  }
  if (chatStore.activeChatType === 'GROUP') {
    return chatStore.activeGroup?.name || 'Nhóm chat'
  }
  const partnerUsername = chatStore.activePartner?.username
  const defaultName = chatStore.activePartner?.fullName || partnerUsername || 'Đồng nghiệp'
  return chatStore.getNickname(partnerUsername, defaultName)
})

// Kiểm tra trạng thái Typing hiển thị ngoài Sidebar
const isColleagueTyping = (username) => {
  if (!username) return false
  const roomId = chatStore.getPrivateRoomId(authStore.user?.username, username)
  const typingList = chatStore.typingUsers[roomId] || []
  return typingList.length > 0
}

const isGroupTyping = (groupId) => {
  const typingList = chatStore.typingUsers[`GROUP_${groupId}`] || []
  return typingList.length > 0
}

const getGroupTypingNames = (groupId) => {
  const typingList = chatStore.typingUsers[`GROUP_${groupId}`] || []
  if (typingList.length === 1) return typingList[0]
  if (typingList.length > 1) return `${typingList[0]} và ${typingList.length - 1} người khác`
  return ''
}

const isPublicTyping = computed(() => {
  const typingList = chatStore.typingUsers['GLOBAL'] || []
  return typingList.length > 0
})

const currentRoomIsTyping = computed(() => {
  const typingList = chatStore.typingUsers[chatStore.activeRoomId] || []
  return typingList.length > 0
})

const activeSubtitle = computed(() => {
  if (chatStore.activeChatType === 'PUBLIC') {
    const typing = chatStore.typingUsers['GLOBAL'] || []
    if (typing.length > 0) return `${typing.join(', ')} đang nhập tin nhắn...`
    return 'Kênh chung • Thảo luận công việc & thông báo nhanh'
  }
  if (chatStore.activeChatType === 'GROUP') {
    const typing = chatStore.typingUsers[`GROUP_${chatStore.activeGroup?.id}`] || []
    if (typing.length > 0) return `${typing.join(', ')} đang nhập...`
    return `${chatStore.activeGroup?.memberCount || 0} thành viên • Trưởng nhóm: ${chatStore.activeGroup?.creatorFullName || chatStore.activeGroup?.creatorUsername}`
  }
  // Chat riêng 1-1
  const partnerUsername = chatStore.activePartner?.username
  const typing = chatStore.typingUsers[chatStore.activeRoomId] || []
  if (typing.length > 0) {
    return 'đang nhập tin nhắn...'
  }
  const fullName = chatStore.activePartner?.fullName
  const hasNickname = chatStore.roomNicknames[chatStore.activeRoomId]?.[partnerUsername]
  if (hasNickname && fullName) {
    return `${fullName} (@${partnerUsername}) • Trực tuyến`
  }
  return `@${partnerUsername || ''} • Trực tuyến`
})

// Danh sách đồng nghiệp chưa tham gia vào nhóm hiện tại
const availableColleaguesForGroup = computed(() => {
  if (!chatStore.activeGroup?.members) return chatStore.colleagues
  const existingUsernames = new Set(chatStore.activeGroup.members.map(m => m.username))
  return chatStore.colleagues.filter(c => !existingUsernames.has(c.username))
})

// Kiểm tra xem user hiện tại có phải là Trưởng nhóm không
const isCurrentUserGroupAdmin = computed(() => {
  if (!chatStore.activeGroup || !authStore.user) return false
  if (chatStore.activeGroup.creatorUsername === authStore.user.username) return true
  const me = chatStore.activeGroup.members?.find(m => m.username === authStore.user.username)
  return me?.role === 'ADMIN'
})

// Lọc danh sách đồng nghiệp theo từ khóa
const filteredColleagues = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return chatStore.colleagues
  return chatStore.colleagues.filter(c => {
    const name = (c.fullName || '').toLowerCase()
    const uname = (c.username || '').toLowerCase()
    return name.includes(kw) || uname.includes(kw)
  })
})

// Kiểm tra xem tin nhắn có phải do mình gửi không
const isMyMessage = (msg) => {
  return msg.senderUsername === authStore.user?.username
}

// Lấy số tin nhắn chưa đọc cho 1 đồng nghiệp cụ thể
const getPrivateUnreadCount = (partnerUsername) => {
  const myUsername = authStore.user?.username
  const roomId = chatStore.getPrivateRoomId(myUsername, partnerUsername)
  return chatStore.unreadCounts[roomId] || 0
}

// Chuyển sang phòng chung
const selectPublicRoom = () => {
  chatStore.selectRoom('PUBLIC')
  nextTick(() => {
    scrollToBottom(false)
    inputTextareaRef.value?.focus()
  })
}

// Chuyển sang chat 1-1 với đồng nghiệp
const selectPrivatePartner = (partner) => {
  chatStore.selectRoom('PRIVATE', partner)
  nextTick(() => {
    scrollToBottom(false)
    inputTextareaRef.value?.focus()
  })
}

// Chuyển sang phòng nhóm
const selectGroupRoom = (group) => {
  chatStore.selectRoom('GROUP', group)
  nextTick(() => {
    scrollToBottom(false)
    inputTextareaRef.value?.focus()
  })
}

// Mở modal tạo nhóm
const openCreateGroupDialog = () => {
  newGroupName.value = ''
  selectedGroupMembers.value = []
  showCreateGroupDialog.value = true
}

// Tạo nhóm chat mới
const handleCreateGroup = async () => {
  const name = newGroupName.value.trim()
  if (!name) {
    ElMessage.warning('Vui lòng nhập tên nhóm chat!')
    return
  }

  isCreatingGroup.value = true
  try {
    await chatStore.createGroup(name, selectedGroupMembers.value)
    ElMessage.success(`Tạo nhóm "${name}" thành công!`)
    showCreateGroupDialog.value = false
    nextTick(() => {
      scrollToBottom(false)
      inputTextareaRef.value?.focus()
    })
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi tạo nhóm chat!')
  } finally {
    isCreatingGroup.value = false
  }
}

// Mở modal quản lý thành viên nhóm
const openGroupMembersModal = () => {
  selectedNewMemberUsernames.value = []
  showGroupMembersModal.value = true
}

// Thêm thành viên vào nhóm
const handleAddMembersToGroup = async () => {
  if (!selectedNewMemberUsernames.value.length || !chatStore.activeGroup) return
  isAddingMembers.value = true
  try {
    await chatStore.addMembersToGroup(chatStore.activeGroup.id, selectedNewMemberUsernames.value)
    ElMessage.success('Đã thêm thành viên mới vào nhóm!')
    selectedNewMemberUsernames.value = []
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi thêm thành viên!')
  } finally {
    isAddingMembers.value = false
  }
}

// Xóa thành viên khỏi nhóm
const handleRemoveMember = async (username, fullName) => {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn xóa "${fullName || username}" khỏi nhóm chat này không?`,
      'Xác nhận xóa thành viên',
      {
        confirmButtonText: 'Xóa khỏi nhóm',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }
    )

    await chatStore.removeMemberFromGroup(chatStore.activeGroup.id, username)
    ElMessage.success(`Đã xóa "${fullName || username}" khỏi nhóm!`)
  } catch (e) {
    // Người dùng hủy bỏ hoặc có lỗi
  }
}

// Rời khỏi nhóm chat
const handleLeaveGroup = async () => {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn rời khỏi nhóm "${chatStore.activeGroup?.name}" không?`,
      'Xác nhận rời nhóm',
      {
        confirmButtonText: 'Rời nhóm',
        cancelButtonText: 'Ở lại',
        type: 'warning'
      }
    )

    const groupName = chatStore.activeGroup?.name
    await chatStore.leaveGroup(chatStore.activeGroup.id)
    showGroupMembersModal.value = false
    ElMessage.info(`Đã rời khỏi nhóm "${groupName}"`)
  } catch (e) {
    // Người dùng hủy bỏ
  }
}

// Bắt đầu chỉnh sửa tin nhắn
const handleStartEdit = (msg) => {
  if (!msg || msg.isDeleted || msg.type !== 'CHAT') return
  editingMessage.value = msg
  inputContent.value = msg.content
  nextTick(() => {
    inputTextareaRef.value?.focus()
  })
}

const cancelEdit = () => {
  editingMessage.value = null
  inputContent.value = ''
}

// Xử lý thu hồi / xóa tin nhắn
const handleDeleteMessage = async (msg) => {
  if (!msg) return
  try {
    await ElMessageBox.confirm(
      'Bạn có chắc chắn muốn thu hồi tin nhắn này không? Tin nhắn sẽ được gỡ bỏ đối với tất cả thành viên trong cuộc trò chuyện.',
      'Xác nhận thu hồi tin nhắn',
      {
        confirmButtonText: 'Thu hồi',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }
    )
    await chatStore.deleteMessage(msg.id)
    ElMessage.success('Đã thu hồi tin nhắn')
  } catch (e) {
    // Người dùng bấm Hủy
  }
}

// Thành viên trong phòng hiện tại để gán biệt danh
const currentRoomMembersForNickname = computed(() => {
  if (chatStore.activeChatType === 'GROUP') {
    return chatStore.activeGroup?.members || []
  }
  if (chatStore.activeChatType === 'PRIVATE' && chatStore.activePartner) {
    return [
      {
        username: authStore.user?.username,
        fullName: authStore.user?.fullName || authStore.user?.username
      },
      {
        username: chatStore.activePartner.username,
        fullName: chatStore.activePartner.fullName || chatStore.activePartner.username
      }
    ]
  }
  return []
})

// Mở modal đặt biệt danh
const openNicknameModal = async () => {
  nicknameInputs.value = {}
  isSavingNickname.value = {}
  await chatStore.fetchNicknames(chatStore.activeRoomId)
  const currentNicks = chatStore.roomNicknames[chatStore.activeRoomId] || {}
  currentRoomMembersForNickname.value.forEach(u => {
    nicknameInputs.value[u.username] = currentNicks[u.username] || ''
  })
  showNicknameModal.value = true
}

// Lưu biệt danh
const handleSaveNickname = async (username) => {
  const nick = (nicknameInputs.value[username] || '').trim()
  isSavingNickname.value[username] = true
  try {
    await chatStore.setNickname(chatStore.activeRoomId, username, nick)
    ElMessage.success(`Đã cập nhật biệt danh cho @${username}`)
  } catch (e) {
    ElMessage.error('Lỗi khi cập nhật biệt danh!')
  } finally {
    isSavingNickname.value[username] = false
  }
}

// Gửi tin nhắn hoặc Lưu chỉnh sửa
const handleSendMessage = async () => {
  const text = inputContent.value.trim()
  if (!text || !chatStore.isConnected) return

  // Nếu đang chỉnh sửa tin nhắn
  if (editingMessage.value) {
    try {
      await chatStore.editMessage(editingMessage.value.id, text)
      ElMessage.success('Đã chỉnh sửa tin nhắn thành công!')
      cancelEdit()
    } catch (e) {
      ElMessage.error('Lỗi khi chỉnh sửa tin nhắn!')
    }
    return
  }

  if (chatStore.activeChatType === 'PUBLIC' || chatStore.activeChatType === 'GROUP') {
    chatStore.sendPublicMessage(text)
  } else if (chatStore.activePartner) {
    chatStore.sendPrivateMessage(chatStore.activePartner.username, text)
  }

  inputContent.value = ''
  nextTick(() => {
    scrollToBottom(true)
    inputTextareaRef.value?.focus()
  })
}

// Gửi tín hiệu Typing
let typingTimer = null
const handleTyping = () => {
  if (typingTimer) clearTimeout(typingTimer)
  chatStore.sendTyping()
  typingTimer = setTimeout(() => {}, 2000)
}

// Chèn nhanh Emoji
const insertEmoji = (emoji) => {
  inputContent.value += emoji
  inputTextareaRef.value?.focus()
}

// Tải lại lịch sử tin nhắn của phòng hiện tại
const refreshCurrentHistory = () => {
  if (chatStore.activeChatType === 'PUBLIC') {
    chatStore.loadRoomHistory('GLOBAL')
  } else if (chatStore.activeChatType === 'GROUP' && chatStore.activeGroup) {
    chatStore.loadRoomHistory(`GROUP_${chatStore.activeGroup.id}`)
  } else if (chatStore.activePartner) {
    chatStore.loadPrivateHistory(chatStore.activePartner.username)
  }
}

// Tự động cuộn xuống dưới cùng
const scrollToBottom = (smooth = true) => {
  nextTick(() => {
    if (messagesScrollRef.value) {
      messagesScrollRef.value.scrollTo({
        top: messagesScrollRef.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
    }
  })
}

// Xử lý sự kiện scroll để hiển thị nút Scroll-to-bottom và Lazy Loading tin cũ
const handleScroll = async () => {
  if (!messagesScrollRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = messagesScrollRef.value
  const distanceFromBottom = scrollHeight - scrollTop - clientHeight
  showScrollToBottom.value = distanceFromBottom > 150

  // Kích hoạt Lazy Loading khi cuộn lên gần đầu danh sách (scrollTop < 40)
  if (scrollTop < 40 && !chatStore.isLoadingOlder && chatStore.hasMoreHistory[chatStore.activeRoomId] !== false) {
    const oldScrollHeight = messagesScrollRef.value.scrollHeight
    await chatStore.loadOlderMessages(chatStore.activeRoomId)
    await nextTick()
    const newScrollHeight = messagesScrollRef.value.scrollHeight
    // Neo lại vị trí cuộn để không bị giật màn hình
    messagesScrollRef.value.scrollTop = newScrollHeight - oldScrollHeight + scrollTop
  }
}

// Format thời gian hiển thị
const formatTime = (isoString) => {
  if (!isoString) return ''
  try {
    const d = new Date(isoString)
    const hours = String(d.getHours()).padStart(2, '0')
    const mins = String(d.getMinutes()).padStart(2, '0')
    return `${hours}:${mins}`
  } catch (e) {
    return ''
  }
}

// Format thời gian trên Sidebar danh bạ (Hôm nay: HH:mm, Hôm qua, Ngày/Tháng)
const formatSidebarTime = (isoString) => {
  if (!isoString) return ''
  try {
    const d = new Date(isoString)
    const now = new Date()
    const isToday = d.toDateString() === now.toDateString()
    if (isToday) {
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      return `${h}:${m}`
    }
    const yesterday = new Date(now)
    yesterday.setDate(yesterday.getDate() - 1)
    if (d.toDateString() === yesterday.toDateString()) {
      return 'Hôm qua'
    }
    return `${d.getDate()}/${d.getMonth() + 1}`
  } catch (e) {
    return ''
  }
}

// Sinh màu ngẫu nhiên hài hòa cho Avatar mặc định
const getAvatarBg = (name) => {
  const colors = [
    'linear-gradient(135deg, #3b82f6, #1d4ed8)',
    'linear-gradient(135deg, #10b981, #047857)',
    'linear-gradient(135deg, #f59e0b, #d97706)',
    'linear-gradient(135deg, #8b5cf6, #6d28d9)',
    'linear-gradient(135deg, #ec4899, #be185d)',
    'linear-gradient(135deg, #06b6d4, #0e7490)'
  ]
  let hash = 0
  for (let i = 0; i < (name || '').length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return { background: colors[Math.abs(hash) % colors.length] }
}

const getAvatarInitials = (name) => {
  if (!name) return 'U'
  const parts = name.trim().split(/\s+/)
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

// Bật / Tắt âm thanh thông báo
const handleToggleSound = () => {
  chatStore.toggleSound()
  if (chatStore.isSoundEnabled) {
    ElMessage.success('Đã bật âm thanh thông báo tin nhắn')
  } else {
    ElMessage.info('Đã tắt âm thanh thông báo')
  }
}

// Bật thông báo Desktop khi chuyển tab
const handleToggleNotification = async () => {
  if (!('Notification' in window)) {
    ElMessage.warning('Trình duyệt của bạn không hỗ trợ Web Notification API')
    return
  }
  if (chatStore.isNotificationEnabled) {
    ElMessage.info('Thông báo màn hình đang hoạt động khi bạn chuyển tab')
    return
  }
  const granted = await chatStore.enableDesktopNotification()
  if (granted) {
    ElMessage.success('Đã kích hoạt thông báo Desktop thành công!')
  } else {
    ElMessage.warning('Quyền thông báo bị chặn trong cài đặt trình duyệt')
  }
}

// Mở trình chọn file ảnh / video
const triggerImageSelect = () => {
  if (isUploadingImage.value || !chatStore.isConnected) return
  imageFileInputRef.value?.click()
}

// Hàm upload và gửi media (ảnh & video)
const uploadAndSendMedia = async (file) => {
  if (!file) return

  const isVideo = file.type.startsWith('video/')
  const isImage = file.type.startsWith('image/')

  if (!isImage && !isVideo) {
    ElMessage.warning('Vui lòng chỉ chọn tệp định dạng hình ảnh hoặc video!')
    return
  }

  // Validate dung lượng: Ảnh < 15MB, Video < 100MB
  if (isImage && file.size > 15 * 1024 * 1024) {
    ElMessage.warning('Kích thước ảnh tối đa cho phép là 15MB!')
    return
  }
  if (isVideo && file.size > 100 * 1024 * 1024) {
    ElMessage.warning('Kích thước video tối đa cho phép là 100MB!')
    return
  }

  isUploadingImage.value = true
  const formData = new FormData()
  formData.append('file', file)

  try {
    ElMessage.info(isVideo ? 'Đang tải video lên Cloud...' : 'Đang tải ảnh lên Cloud...')
    // Không truyền thủ công Content-Type để Axios tự sinh boundary multipart chuẩn
    const res = await api.post('/files/upload', formData)

    const mediaUrl = res.data?.url
    const resourceType = res.data?.resourceType || (isVideo ? 'VIDEO' : 'IMAGE')
    if (!mediaUrl) {
      throw new Error('Không nhận được đường dẫn tệp từ Cloudinary')
    }

    if (resourceType === 'VIDEO') {
      if (chatStore.activeChatType === 'PUBLIC') {
        chatStore.sendPublicVideo(mediaUrl, 'GLOBAL')
      } else if (chatStore.activeChatType === 'GROUP' && chatStore.activeGroup) {
        chatStore.sendPublicVideo(mediaUrl, `GROUP_${chatStore.activeGroup.id}`)
      } else if (chatStore.activePartner) {
        chatStore.sendPrivateVideo(chatStore.activePartner.username, mediaUrl)
      }
      ElMessage.success('Đã gửi video thành công!')
    } else {
      if (chatStore.activeChatType === 'PUBLIC') {
        chatStore.sendPublicImage(mediaUrl, 'GLOBAL')
      } else if (chatStore.activeChatType === 'GROUP' && chatStore.activeGroup) {
        chatStore.sendPublicImage(mediaUrl, `GROUP_${chatStore.activeGroup.id}`)
      } else if (chatStore.activePartner) {
        chatStore.sendPrivateImage(chatStore.activePartner.username, mediaUrl)
      }
      ElMessage.success('Đã gửi hình ảnh thành công!')
    }

    nextTick(() => {
      scrollToBottom(true)
    })
  } catch (err) {
    console.error('Lỗi khi tải media:', err)
    ElMessage.error(err.response?.data?.message || err.message || 'Lỗi khi tải tệp lên Cloud!')
  } finally {
    isUploadingImage.value = false
  }
}

// Xử lý chọn file media từ máy
const handleMediaSelected = async (event) => {
  const file = event.target.files?.[0]
  if (file) {
    await uploadAndSendMedia(file)
  }
  event.target.value = ''
}

// Xử lý dán (Paste / Ctrl+V) hình ảnh / video trực tiếp vào ô chat
const handlePaste = async (event) => {
  const items = (event.clipboardData || window.clipboardData)?.items
  if (!items) return

  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item.type.indexOf('image') !== -1 || item.type.indexOf('video') !== -1) {
      event.preventDefault()
      const file = item.getAsFile()
      if (file) {
        await uploadAndSendMedia(file)
        return
      }
    }
  }
}

// Bắt đầu Reply tin nhắn
const handleStartReply = (msg) => {
  chatStore.setReplyingTo(msg)
  nextTick(() => {
    inputTextareaRef.value?.focus()
  })
}

// Cuộn đến tin nhắn gốc khi bấm vào quote Reply
const scrollToMessage = (messageId) => {
  if (!messageId) return
  const el = document.getElementById(`msg-${messageId}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.classList.add('is-highlighted')
    setTimeout(() => {
      el.classList.remove('is-highlighted')
    }, 2000)
  } else {
    ElMessage.info('Tin nhắn này ở xa hơn trong lịch sử.')
  }
}

// Mở modal xem trước ảnh phóng to
const openImageLightbox = (url) => {
  previewImageUrl.value = url
}

// Phím tắt ESC đóng lightbox
const handleKeydown = (e) => {
  if (e.key === 'Escape' && previewImageUrl.value) {
    previewImageUrl.value = null
  }
}

// Watch số lượng tin nhắn để tự động cuộn (chỉ khi có tin nhắn mới, không cuộn khi lazy load tin cũ)
watch(
  () => chatStore.currentMessages.length,
  () => {
    if (!chatStore.isLoadingOlder) {
      scrollToBottom(true)
    }
  }
)

const handleWindowFocus = () => {
  if (chatStore.activeRoomId && !document.hidden) {
    if (chatStore.unreadCounts[chatStore.activeRoomId] > 0) {
      chatStore.markAsRead(chatStore.activeRoomId)
    }
  }
}

onMounted(() => {
  // Đảm bảo WebSocket STOMP đã kích hoạt
  chatStore.connect()
  // Tải danh bạ đồng nghiệp
  chatStore.fetchColleagues()
  // Tải danh sách nhóm chat của tôi
  chatStore.fetchMyGroups()
  // Mở mặc định phòng chung
  selectPublicRoom()
  // Lắng nghe sự kiện ESC để đóng lightbox
  window.addEventListener('keydown', handleKeydown)
  // Lắng nghe sự kiện quay lại tab hoặc focus cửa sổ để cập nhật đã đọc
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('visibilitychange', handleWindowFocus)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('visibilitychange', handleWindowFocus)
})
</script>

<style scoped>
.realtime-chat-page {
  height: calc(100vh - 120px);
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* KHUNG CHAT FULL HEIGHT CONTAINER */
.chat-card-container {
  display: flex;
  height: 100%;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

/* =========================================================================
   CỘT TRÁI: SIDEBAR DANH BẠ HỘI THOẠI
   ========================================================================= */
.chat-sidebar {
  width: 320px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px 16px 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
}

.sidebar-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

/* Badge trạng thái WebSocket */
.ws-status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.status-online {
  background: #ecfdf5;
  color: #059669;
}

.status-online .status-pulse {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

.status-connecting {
  background: #fffbeb;
  color: #d97706;
}

.status-connecting .status-pulse {
  background: #f59e0b;
}

.status-offline {
  background: #fef2f2;
  color: #dc2626;
}

.status-offline .status-pulse {
  background: #ef4444;
}

.status-pulse {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

/* Ô Search */
.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: #94a3b8;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 8px 30px 8px 32px;
  background: #f1f5f9;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  transition: all 0.2s;
}

.search-input:focus {
  background: #ffffff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.search-clear-btn {
  position: absolute;
  right: 8px;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 12px;
}

/* Danh sách cuộn */
.directory-list-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 10px 8px;
}

.section-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 10px 6px 10px;
}

.section-label {
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

.btn-refresh-colleagues {
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-refresh-colleagues:hover {
  background: #e2e8f0;
  color: #0f172a;
}

/* Item danh bạ */
.directory-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
  margin-bottom: 2px;
}

.directory-item:hover {
  background: #f1f5f9;
}

.directory-item.is-active {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}

.item-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.channel-avatar {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
}

.default-avatar {
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.online-indicator-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #10b981;
  border: 2px solid #ffffff;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 3px;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-tag-public {
  font-size: 10px;
  font-weight: 600;
  background: #e0e7ff;
  color: #4338ca;
  padding: 2px 6px;
  border-radius: 10px;
}

.role-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 10px;
}

.role-admin {
  background: #fee2e2;
  color: #b91c1c;
}

.role-user {
  background: #e0f2fe;
  color: #0369a1;
}

.item-preview {
  font-size: 12px;
  color: #64748b;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-preview.is-unread-bold {
  font-weight: 600;
  color: #0f172a !important;
}

.item-time {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
  flex-shrink: 0;
  margin-left: 6px;
}

.unread-pill {
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 5px;
  border-radius: 9999px;
  text-align: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.35);
  flex-shrink: 0;
}

.empty-colleagues {
  padding: 20px 10px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}

/* =========================================================================
   CỘT PHẢI: KHUNG TRÒ CHUYỆN CHÍNH
   ========================================================================= */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background: #fdfefe;
}

.main-header {
  height: 64px;
  padding: 0 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
}

.current-room-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-room-avatar-wrapper {
  position: relative;
}

.header-room-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.header-title-group {
  display: flex;
  flex-direction: column;
}

.title-with-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-chat-name {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.badge-tag-channel {
  font-size: 11px;
  font-weight: 600;
  background: #e0e7ff;
  color: #4338ca;
  padding: 2px 8px;
  border-radius: 12px;
}

.badge-tag-private {
  font-size: 11px;
  font-weight: 600;
  background: #dcfce7;
  color: #15803d;
  padding: 2px 8px;
  border-radius: 12px;
}

.current-chat-desc {
  font-size: 12px;
  color: #64748b;
  margin: 2px 0 0 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-icon-action {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid #e2e8f0;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.btn-icon-action:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-icon-action.is-muted {
  color: #ef4444;
  background: #fef2f2;
  border-color: #fecaca;
}

.btn-icon-action.is-notif-active {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.notif-active-dot {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 7px;
  height: 7px;
  background: #10b981;
  border-radius: 50%;
  border: 1.5px solid #ffffff;
}

/* =========================================================================
   MESSAGES VIEWPORT & BUBBLES
   ========================================================================= */
.messages-viewport {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8fafc;
}

/* Welcome banner */
.room-welcome-banner {
  text-align: center;
  padding: 24px 20px 20px 20px;
  margin-bottom: 10px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
}

.welcome-icon-box {
  width: 52px;
  height: 52px;
  margin: 0 auto 10px auto;
  border-radius: 50%;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.welcome-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  max-width: 480px;
  margin: 0 auto;
}

/* Message rows */
.message-bubble-row {
  display: flex;
  gap: 10px;
  max-width: 75%;
}

.message-bubble-row.is-incoming {
  align-self: flex-start;
}

.message-bubble-row.is-outgoing {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.sender-avatar-col {
  flex-shrink: 0;
  margin-top: 4px;
}

.bubble-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
}

.bubble-content-col {
  display: flex;
  flex-direction: column;
}

.sender-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  margin-left: 2px;
}

.sender-name {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.sender-username {
  font-size: 11px;
  color: #94a3b8;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  position: relative;
}

/* Tin nhắn của người khác (Incoming) */
.is-incoming .message-bubble {
  background: #ffffff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* Tin nhắn của mình (Outgoing) */
.is-outgoing .message-bubble {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

.bubble-text {
  white-space: pre-wrap;
}

.bubble-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 4px;
  font-size: 10px;
}

.is-incoming .meta-time {
  color: #94a3b8;
}

.is-outgoing .meta-time {
  color: #bfdbfe;
}

.is-outgoing .meta-status-check {
  color: #bfdbfe;
  display: flex;
  align-items: center;
}

/* Typing Indicator */
.typing-indicator-row {
  align-self: flex-start;
  margin-top: 4px;
}

.typing-bubble {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  font-size: 12px;
  color: #64748b;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.bouncing-dots {
  display: inline-flex;
  gap: 3px;
}

.dot {
  width: 5px;
  height: 5px;
  background: #3b82f6;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Sidebar Typing Indicator */
.sidebar-typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #10b981;
  font-weight: 600;
  min-height: 18px;
}

.sidebar-typing-text {
  font-style: italic;
  color: #10b981;
}

.typing-dots-mini {
  display: inline-flex;
  gap: 2.5px;
  align-items: center;
}

.typing-dots-mini span {
  width: 4px;
  height: 4px;
  background: #10b981;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-dots-mini span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots-mini span:nth-child(2) { animation-delay: -0.16s; }

/* Header Typing status */
.current-chat-desc.is-typing-desc {
  color: #10b981 !important;
  font-weight: 600 !important;
}

.typing-dot-blink {
  display: inline-block;
  color: #10b981;
  font-size: 9px;
  margin-right: 3px;
  animation: blink-dot 1s infinite alternate;
}

@keyframes blink-dot {
  from { opacity: 0.3; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1.2); }
}

/* Nút Scroll to bottom */
.btn-scroll-bottom {
  position: absolute;
  bottom: 120px;
  right: 24px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #2563eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 10;
}

.btn-scroll-bottom:hover {
  background: #2563eb;
  color: #ffffff;
  transform: translateY(-2px);
}

/* =========================================================================
   FOOTER SOẠN THẢO VĂN BẢN
   ========================================================================= */
.main-footer {
  padding: 10px 16px 14px 16px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}

.quick-reactions-bar {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.btn-quick-emoji {
  background: #f1f5f9;
  border: none;
  font-size: 15px;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-quick-emoji:hover {
  background: #e2e8f0;
  transform: scale(1.15);
}

.chat-input-form {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.chat-textarea {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.4;
  color: #1e293b;
  resize: none;
  outline: none;
  transition: all 0.2s;
  font-family: inherit;
  background: #f8fafc;
}

.chat-textarea:focus {
  background: #ffffff;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.btn-send-message {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  height: 42px;
}

.btn-send-message:hover:not(:disabled) {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  transform: translateY(-1px);
}

.btn-send-message:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* =========================================================================
   TẢI VÀ ĐÍNH KÈM HÌNH ẢNH
   ========================================================================= */
.hidden-file-input {
  display: none;
}

.btn-attach-image {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.btn-attach-image:hover:not(:disabled) {
  background: #e2e8f0;
  color: #1e293b;
  border-color: #94a3b8;
}

.btn-attach-image:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.upload-spinner-icon {
  width: 18px;
  height: 18px;
  border: 2px solid #cbd5e1;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* =========================================================================
   BONG BÓNG TIN NHẮN HÌNH ẢNH
   ========================================================================= */
.message-bubble.is-image-bubble {
  padding: 6px;
  max-width: 320px;
}

.bubble-image-wrapper {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  display: block;
  background: #0f172a;
}

.bubble-image {
  display: block;
  width: 100%;
  max-height: 260px;
  object-fit: cover;
  border-radius: 10px;
  transition: transform 0.2s ease, filter 0.2s ease;
}

.image-zoom-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.2s ease;
  backdrop-filter: blur(2px);
}

.bubble-image-wrapper:hover .image-zoom-overlay {
  opacity: 1;
}

.bubble-image-wrapper:hover .bubble-image {
  transform: scale(1.02);
}

.is-image-bubble .bubble-meta {
  padding: 4px 6px 2px 6px;
}

/* =========================================================================
   LIGHTBOX PREVIEW FULLSCREEN MODAL
   ========================================================================= */
.image-lightbox-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(6px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  animation: fadeIn 0.2s ease;
}

.lightbox-dialog {
  background: #ffffff;
  border-radius: 16px;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4);
  overflow: hidden;
  animation: zoomIn 0.2s ease;
}

.lightbox-header {
  padding: 12px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.lightbox-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.lightbox-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-lightbox-tool {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s;
}

.btn-lightbox-tool:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-lightbox-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: transparent;
  border: none;
  font-size: 18px;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-lightbox-close:hover {
  background: #fee2e2;
  color: #dc2626;
}

.lightbox-body {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f172a;
  overflow: auto;
}

.lightbox-img {
  max-width: 82vw;
  max-height: 75vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes zoomIn {
  from { transform: scale(0.95); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

/* =========================================================================
   LAZY LOADING INDICATORS
   ========================================================================= */
.lazy-loading-spinner-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 12px;
  color: #64748b;
}

.lazy-spinner-icon {
  width: 14px;
  height: 14px;
  border: 2px solid #cbd5e1;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.lazy-end-banner {
  text-align: center;
  padding: 8px 0;
  font-size: 11px;
  color: #94a3b8;
  border-bottom: 1px dashed #e2e8f0;
  margin-bottom: 8px;
}

/* =========================================================================
   BUBBLE AND HOVER ACTION TOOLBAR
   ========================================================================= */
.bubble-and-actions {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
}

.is-outgoing .bubble-and-actions {
  flex-direction: row-reverse;
}

.message-actions-hover {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease, transform 0.15s ease;
  transform: scale(0.95);
  z-index: 5;
}

.message-bubble-row:hover .message-actions-hover {
  opacity: 1;
  pointer-events: auto;
  transform: scale(1);
}

.btn-hover-reaction {
  background: transparent;
  border: none;
  font-size: 14px;
  padding: 2px 4px;
  cursor: pointer;
  border-radius: 4px;
  transition: transform 0.1s ease;
}

.btn-hover-reaction:hover {
  transform: scale(1.3);
}

.btn-hover-reply {
  background: transparent;
  border: none;
  color: #64748b;
  padding: 3px 5px;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}

.btn-hover-reply:hover {
  color: #2563eb;
  background: #f1f5f9;
}

/* =========================================================================
   REPLY QUOTE BLOCK IN BUBBLE
   ========================================================================= */
.bubble-reply-quote {
  display: flex;
  gap: 8px;
  padding: 6px 10px;
  margin-bottom: 6px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: background 0.15s;
}

.is-outgoing .bubble-reply-quote {
  background: rgba(255, 255, 255, 0.15);
}

.bubble-reply-quote:hover {
  background: rgba(0, 0, 0, 0.08);
}

.is-outgoing .bubble-reply-quote:hover {
  background: rgba(255, 255, 255, 0.25);
}

.quote-bar {
  width: 3px;
  background: #3b82f6;
  border-radius: 3px;
  flex-shrink: 0;
}

.is-outgoing .quote-bar {
  background: #93c5fd;
}

.quote-text-group {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.quote-sender {
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  line-height: 1.2;
}

.is-outgoing .quote-sender {
  color: #dbeafe;
}

.quote-snippet {
  font-size: 12px;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 240px;
}

.is-outgoing .quote-snippet {
  color: #e0f2fe;
}

/* Highlight tin nhắn khi scroll tới */
@keyframes highlightPulse {
  0% { background: rgba(59, 130, 246, 0.25); border-radius: 12px; }
  100% { background: transparent; }
}

.is-highlighted {
  animation: highlightPulse 2s ease;
}

/* =========================================================================
   REACTION CHIPS ATTACHED TO BUBBLE
   ========================================================================= */
.bubble-reactions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}

.reaction-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 7px;
  border-radius: 12px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  line-height: 1.2;
}

.reaction-chip:hover {
  background: #e2e8f0;
  transform: scale(1.05);
}

.reaction-chip.is-my-reaction {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #1d4ed8;
}

.reaction-emoji {
  font-size: 13px;
}

.reaction-count {
  font-weight: 700;
  font-size: 11px;
}

/* =========================================================================
   REPLY PREVIEW BAR (ABOVE INPUT)
   ========================================================================= */
.reply-preview-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-left: 3px solid #2563eb;
  border-radius: 8px;
  margin-bottom: 8px;
  animation: fadeIn 0.15s ease;
}

.reply-preview-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.reply-preview-icon {
  color: #2563eb;
  display: flex;
  align-items: center;
}

.reply-preview-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.reply-to-user {
  font-size: 12px;
  color: #1e293b;
}

.reply-to-content {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 480px;
}

.btn-cancel-reply {
  background: transparent;
  border: none;
  font-size: 15px;
  color: #94a3b8;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}

.btn-cancel-reply:hover {
  color: #ef4444;
  background: #fee2e2;
}

/* Optimistic sending status */
.is-pending {
  opacity: 0.82;
}

.status-sending-spinner {
  display: inline-block;
  width: 10px;
  height: 10px;
  border: 1.5px solid #bfdbfe;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* =========================================================================
   NHÓM CHAT (GROUP CHAT) & MODAL STYLES
   ========================================================================= */

.section-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px 6px 12px;
}

.section-label-row .section-label {
  padding: 0;
}

.btn-create-group,
.btn-refresh-colleagues {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-create-group:hover,
.btn-refresh-colleagues:hover {
  background: #2563eb;
  border-color: #2563eb;
  color: #ffffff;
}

.empty-groups-hint {
  padding: 12px;
  margin: 4px 8px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  text-align: center;
  font-size: 12px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.btn-link-create {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 4px;
}

.btn-link-create:hover {
  text-decoration: underline;
}

.group-avatar-box {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.item-tag-group {
  font-size: 11px;
  color: #6366f1;
  background: #eef2ff;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 600;
}

.badge-tag-group {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 600;
  color: #4338ca;
  background: #e0e7ff;
  padding: 2px 8px;
  border-radius: 12px;
}

.btn-group-manage-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-group-manage-action:hover {
  background: #e2e8f0;
  color: #1e293b;
}

/* MODAL OVERLAY & CARD */
.modal-backdrop-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  animation: fadeIn 0.2s ease;
}

.custom-modal-card {
  width: 100%;
  max-width: 480px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: zoomIn 0.2s ease;
}

.custom-modal-card.modal-large {
  max-width: 620px;
}

.modal-card-header {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.header-icon-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-header-icon {
  font-size: 22px;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-title-group {
  display: flex;
  flex-direction: column;
}

.modal-card-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.modal-card-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.btn-close-modal {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: transparent;
  border: none;
  font-size: 16px;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-close-modal:hover {
  background: #fee2e2;
  color: #ef4444;
}

.modal-card-body {
  padding: 20px;
  overflow-y: auto;
  max-height: calc(85vh - 140px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.required-star {
  color: #ef4444;
}

.form-text-input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.form-text-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.colleagues-checkbox-scroll {
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 6px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.colleague-check-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  transition: background 0.15s;
}

.colleague-check-item:hover {
  background: #f1f5f9;
}

.check-box-input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.check-avatar {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.check-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.check-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.check-username {
  font-size: 11px;
  color: #64748b;
}

.modal-card-footer {
  padding: 14px 20px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.modal-footer-between {
  justify-content: space-between;
}

.btn-modal-cancel {
  padding: 8px 16px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-cancel:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.btn-modal-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 8px;
  background: #2563eb;
  border: 1px solid #2563eb;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-primary:hover:not(:disabled) {
  background: #1d4ed8;
  border-color: #1d4ed8;
}

.btn-modal-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #bfdbfe;
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.btn-leave-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-leave-group:hover {
  background: #fee2e2;
  border-color: #f87171;
}

/* ADD MEMBERS IN MODAL */
.add-members-section {
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sub-section-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.add-members-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.colleagues-select-scroll {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 110px;
  overflow-y: auto;
  padding: 4px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}

.colleague-check-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 14px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.colleague-check-chip:hover {
  background: #e2e8f0;
}

.colleague-check-chip input {
  margin: 0;
  cursor: pointer;
}

.btn-submit-add-members {
  align-self: flex-start;
  padding: 6px 14px;
  border-radius: 6px;
  background: #2563eb;
  border: none;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-submit-add-members:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-submit-add-members:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* CURRENT MEMBERS LIST IN MODAL */
.current-members-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.members-table-scroll {
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

.member-row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s;
}

.member-row-item:last-child {
  border-bottom: none;
}

.member-row-item:hover {
  background: #f8fafc;
}

.member-left-col {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.member-info-col {
  display: flex;
  flex-direction: column;
}

.member-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.member-role-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 8px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.badge-role-admin {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
}

.badge-role-member {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.member-username {
  font-size: 11px;
  color: #94a3b8;
}

.member-right-actions {
  display: flex;
  align-items: center;
}

.btn-remove-member {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  background: transparent;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-remove-member:hover {
  background: #fee2e2;
  color: #b91c1c;
}

/* =========================================================================
   SIDEBAR STATUS ICON & PREVIEW ROW
   ========================================================================= */
.item-preview-row {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.sidebar-status-icon {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
}

.sidebar-status-icon.status-read {
  color: #2563eb;
}

.sidebar-status-icon.status-sent {
  color: #94a3b8;
}

/* =========================================================================
   DELETED MESSAGE BUBBLE
   ========================================================================= */
.message-bubble.is-deleted-bubble {
  background: #f1f5f9 !important;
  color: #94a3b8 !important;
  border: 1px dashed #cbd5e1 !important;
  box-shadow: none !important;
}

.is-outgoing .message-bubble.is-deleted-bubble {
  background: #f8fafc !important;
  color: #94a3b8 !important;
  border-color: #cbd5e1 !important;
}

.bubble-deleted-box {
  display: flex;
  align-items: center;
  gap: 6px;
  font-style: italic;
  font-size: 13px;
  color: #94a3b8;
  padding: 2px 4px;
}

.deleted-msg-text {
  font-style: italic;
}

/* =========================================================================
   VIDEO MESSAGE BUBBLE
   ========================================================================= */
.message-bubble.is-video-bubble {
  padding: 6px;
  max-width: 380px;
}

.bubble-video-wrapper {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  background: #0f172a;
}

.bubble-video {
  display: block;
  width: 100%;
  max-height: 280px;
  border-radius: 10px;
  outline: none;
}

/* =========================================================================
   READ & SENT CHECKMARKS
   ========================================================================= */
.status-read-double-check {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #38bdf8;
}

.read-status-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: #38bdf8;
}

.seen-pill {
  font-size: 11px;
  font-weight: 600;
  color: #0284c7;
  background: #e0f2fe;
  padding: 2px 7px;
  border-radius: 9999px;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-sent-single-check {
  display: inline-flex;
  align-items: center;
  color: rgba(255, 255, 255, 0.7);
}

.is-incoming .status-sent-single-check {
  color: #94a3b8;
}

.meta-edited-tag {
  font-size: 10px;
  font-style: italic;
  opacity: 0.8;
  margin-right: 4px;
}

/* =========================================================================
   HOVER ACTIONS: EDIT & DELETE BUTTONS
   ========================================================================= */
.btn-hover-edit,
.btn-hover-delete {
  background: transparent;
  border: none;
  color: #64748b;
  padding: 3px 5px;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.btn-hover-edit:hover {
  color: #2563eb;
  background: #f1f5f9;
}

.btn-hover-delete:hover {
  color: #dc2626;
  background: #fee2e2;
}

/* =========================================================================
   EDIT PREVIEW BAR (ABOVE INPUT)
   ========================================================================= */
.edit-preview-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-left: 3px solid #f59e0b;
  border-radius: 8px;
  margin-bottom: 8px;
  animation: fadeIn 0.15s ease;
}

.edit-preview-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.edit-preview-icon {
  color: #d97706;
  display: flex;
  align-items: center;
}

.edit-preview-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.edit-to-user {
  font-size: 12px;
  font-weight: 700;
  color: #b45309;
}

.edit-to-content {
  font-size: 12px;
  color: #78350f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 480px;
}

.btn-cancel-edit {
  background: transparent;
  border: none;
  font-size: 15px;
  color: #92400e;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}

.btn-cancel-edit:hover {
  color: #b91c1c;
  background: #fef3c7;
}

/* =========================================================================
   NICKNAME MODAL STYLES
   ========================================================================= */
.nickname-list-scroll {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow-y: auto;
  padding: 4px 2px;
}

.nickname-row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.15s;
}

.nickname-row-item:hover {
  background: #ffffff;
  border-color: #cbd5e1;
}

.nickname-user-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 130px;
}

.nickname-avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nickname-user-names {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.nickname-user-fullname {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.tag-me {
  font-size: 11px;
  color: #2563eb;
  font-weight: normal;
}

.nickname-user-sub {
  font-size: 11px;
  color: #94a3b8;
}

.nickname-input-box {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 280px;
}

.nickname-input {
  flex: 1;
  padding: 6px 10px;
  font-size: 12px;
}

.btn-save-nickname {
  padding: 6px 14px;
  border-radius: 6px;
  background: #2563eb;
  border: none;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-save-nickname:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-save-nickname:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>

