<template>
  <div class="realtime-chat-page">
    <!-- KHUNG CHAT TỔNG HỢP (SPLIT LAYOUT) -->
    <div class="chat-card-container">
      
      <!-- CỘT TRÁI: DANH BẠ KÊNH & ĐỒNG NGHIỆP -->
      <aside class="chat-sidebar" :class="{ 'is-mobile-hidden': showMobileChat }">
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

            <!-- Nút Ghim Kênh Chung -->
            <button 
              type="button" 
              class="btn-pin-room" 
              :class="{ 'is-pinned': isRoomPinned('GLOBAL') }"
              :title="isRoomPinned('GLOBAL') ? 'Bỏ ghim phòng chung' : 'Ghim phòng chung lên đầu'"
              @click.stop="togglePinRoom('GLOBAL')"
            >
              <svg viewBox="0 0 24 24" width="13" height="13" :fill="isRoomPinned('GLOBAL') ? '#f59e0b' : 'none'" stroke="currentColor" stroke-width="2">
                <path d="M16 4l1 1v2l-3 3v5l-2 2-2-2V10L7 7V5l1-1h8m0-2H8c-1.1 0-2 .9-2 2v2.59L8.41 9H8v5.59L6 16.59V18h5v5l1 1 1-1v-5h5v-1.41l-2-2V9h-.41L18 6.59V4c0-1.1-.9-2-2-2z"/>
              </svg>
            </button>

            <span v-if="chatStore.unreadCounts['GLOBAL']" class="unread-pill">
              {{ chatStore.unreadCounts['GLOBAL'] }}
            </span>
          </div>

          <!-- PHẦN 2: NHÓM CHAT (GROUP CHAT) -->
          <div class="section-label-row">
            <span class="section-label">NHÓM CHAT ({{ chatStore.myGroups.length }})</span>
            <button type="button" class="btn-create-group-pill" title="Tạo nhóm chat mới" @click="openCreateGroupDialog">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              <span>Tạo nhóm</span>
            </button>
          </div>

          <div 
            v-for="grp in sortedMyGroups" 
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
            <!-- Nút Ghim Nhóm -->
            <button 
              type="button" 
              class="btn-pin-room" 
              :class="{ 'is-pinned': isRoomPinned('GROUP_' + grp.id) }"
              :title="isRoomPinned('GROUP_' + grp.id) ? 'Bỏ ghim nhóm này' : 'Ghim nhóm lên đầu'"
              @click.stop="togglePinRoom('GROUP_' + grp.id)"
            >
              <svg viewBox="0 0 24 24" width="13" height="13" :fill="isRoomPinned('GROUP_' + grp.id) ? '#f59e0b' : 'none'" stroke="currentColor" stroke-width="2">
                <path d="M16 4l1 1v2l-3 3v5l-2 2-2-2V10L7 7V5l1-1h8m0-2H8c-1.1 0-2 .9-2 2v2.59L8.41 9H8v5.59L6 16.59V18h5v5l1 1 1-1v-5h5v-1.41l-2-2V9h-.41L18 6.59V4c0-1.1-.9-2-2-2z"/>
              </svg>
            </button>

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
            <div class="section-actions-right">
              <button 
                v-if="chatStore.totalUnreadCount > 0" 
                type="button"
                class="btn-mark-all-read" 
                title="Đánh dấu tất cả tin nhắn đã đọc"
                @click.stop="handleMarkAllRead"
              >
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2 13l4 4L16 7M8 13l4 4L22 7" />
                </svg>
                <span>Đã đọc hết</span>
              </button>
              <button type="button" class="btn-refresh-colleagues" title="Làm mới danh sách" @click="chatStore.fetchColleagues">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="filteredColleagues.length === 0" class="empty-colleagues">
            <p>Không tìm thấy đồng nghiệp phù hợp</p>
          </div>

          <div 
            v-for="colleague in sortedFilteredColleagues" 
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
              <span 
                v-if="chatStore.isUserOnline(colleague.username)" 
                class="online-indicator-dot is-online"
                title="Đang trực tuyến"
              ></span>
            </div>

            <div class="item-info">
              <div class="item-top-row">
                <span class="item-name">{{ getColleagueDisplayName(colleague) }}</span>
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

            <!-- Nút Ghim Đồng Nghiệp -->
            <button 
              type="button" 
              class="btn-pin-room" 
              :class="{ 'is-pinned': isRoomPinned(colleague.username) }"
              :title="isRoomPinned(colleague.username) ? 'Bỏ ghim đồng nghiệp này' : 'Ghim đồng nghiệp lên đầu'"
              @click.stop="togglePinRoom(colleague.username)"
            >
              <svg viewBox="0 0 24 24" width="13" height="13" :fill="isRoomPinned(colleague.username) ? '#f59e0b' : 'none'" stroke="currentColor" stroke-width="2">
                <path d="M16 4l1 1v2l-3 3v5l-2 2-2-2V10L7 7V5l1-1h8m0-2H8c-1.1 0-2 .9-2 2v2.59L8.41 9H8v5.59L6 16.59V18h5v5l1 1 1-1v-5h5v-1.41l-2-2V9h-.41L18 6.59V4c0-1.1-.9-2-2-2z"/>
              </svg>
            </button>
          </div>

        </div>
      </aside>

      <!-- CỘT PHẢI: KHUNG HỘI THOẠI TRỰC TUYẾN CHÍNH -->
      <main class="chat-main" :class="{ 'is-mobile-hidden': !showMobileChat }" :style="chatMainStyle">
        
        <!-- Header khung hội thoại -->
        <header class="main-header">
          <div class="current-room-info">
            <!-- Nút quay lại danh bạ trên giao diện Mobile -->
            <button type="button" class="btn-mobile-back" title="Quay lại danh sách chat" @click="backToSidebar">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="15 18 9 12 15 6"></polyline>
              </svg>
            </button>

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
              <span 
                v-if="chatStore.activeChatType === 'PRIVATE' && chatStore.isUserOnline(chatStore.activePartner?.username)" 
                class="online-indicator-dot is-online"
                title="Đang trực tuyến"
              ></span>
            </div>

            <div class="header-title-group">
              <div class="title-with-badge">
                <h3 class="current-chat-name">{{ activeTitle }}</h3>
                <span v-if="chatStore.activeChatType === 'PUBLIC'" class="badge-tag-channel">Phòng chung</span>
                <span v-else-if="chatStore.activeChatType === 'GROUP'" class="badge-tag-group">Nhóm ({{ chatStore.activeGroup?.memberCount }} tv)</span>
                <!-- Bổ sung Status Badge cho chat 1-1 -->
                <span 
                  v-else-if="chatStore.activeChatType === 'PRIVATE'" 
                  class="presence-status-pill"
                  :class="chatStore.isUserOnline(chatStore.activePartner?.username) ? 'is-online' : 'is-offline'"
                >
                  <span class="presence-pulse-dot"></span>
                  {{ chatStore.isUserOnline(chatStore.activePartner?.username) ? 'Trực tuyến' : 'Ngoại tuyến' }}
                </span>
              </div>
              <p class="current-chat-desc" :class="{ 'is-typing-desc': currentRoomIsTyping }">
                <span v-if="currentRoomIsTyping" class="typing-dot-blink">●</span>
                {{ activeSubtitle }}
              </p>
            </div>
          </div>

          <!-- Nút hành động nhanh trên Header (Đã thu gọn cực kỳ tinh tế & gọn gàng) -->
          <div class="header-actions">
            <!-- Nút Gọi thoại nội bộ (Audio Call) -->
            <button 
              v-if="chatStore.activeChatType === 'PRIVATE'"
              type="button" 
              class="btn-icon-action btn-header-call btn-header-voice-call"
              title="Gọi thoại nội bộ (Audio Call)"
              @click="startVoiceCall"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
              </svg>
            </button>

            <!-- Nút Gọi video nội bộ (Video Call) -->
            <button 
              v-if="chatStore.activeChatType === 'PRIVATE'"
              type="button" 
              class="btn-icon-action btn-header-call btn-header-video-call"
              title="Gọi video nội bộ (Video Call)"
              @click="startVideoCall"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="23 7 16 12 23 17 23 7"></polygon>
                <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
              </svg>
            </button>

            <!-- Nút Tìm kiếm tin nhắn trong đoạn chat -->
            <button 
              class="btn-icon-action btn-header-search-toggle" 
              :class="{ 'is-search-active': isSearchOpen }"
              title="Tìm kiếm tin nhắn trong cuộc trò chuyện (Ctrl+F)"
              @click="toggleSearchBar"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </button>

            <!-- Nút Ngăn lưu trữ Media & Tài liệu (Shared Drawer) -->
            <button 
              class="btn-icon-action btn-header-drawer-toggle" 
              :class="{ 'is-drawer-open': showMediaDrawer }"
              title="Media, Tệp tin & Liên kết đã chia sẻ"
              @click="showMediaDrawer = !showMediaDrawer"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="15" y1="3" x2="15" y2="21"></line>
              </svg>
            </button>

            <!-- Menu Tùy chọn Cuộc trò chuyện (Consolidated Dropdown Menu) -->
            <div class="header-more-menu-wrapper">
              <button 
                type="button" 
                class="btn-icon-action btn-header-more"
                :class="{ 'is-active': isHeaderMenuOpen }"
                title="Tùy chọn cuộc trò chuyện"
                @click.stop="isHeaderMenuOpen = !isHeaderMenuOpen"
              >
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                  <circle cx="12" cy="5" r="2"></circle>
                  <circle cx="12" cy="12" r="2"></circle>
                  <circle cx="12" cy="19" r="2"></circle>
                </svg>
              </button>

              <!-- Dropdown Menu Header -->
              <transition name="popover-fade">
                <div v-if="isHeaderMenuOpen" class="header-dropdown-menu" @click.stop>
                  <!-- 0. Tìm kiếm & Media cho Mobile -->
                  <button type="button" class="header-menu-item header-menu-mobile-only" @click="toggleSearchBar(); isHeaderMenuOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="11" cy="11" r="8"></circle>
                      <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                    <span>Tìm kiếm tin nhắn</span>
                  </button>

                  <button type="button" class="header-menu-item header-menu-mobile-only" @click="showMediaDrawer = !showMediaDrawer; isHeaderMenuOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                      <line x1="15" y1="3" x2="15" y2="21"></line>
                    </svg>
                    <span>Tệp & Media đã chia sẻ</span>
                  </button>

                  <!-- 1. Đổi màu chủ đề -->
                  <button type="button" class="header-menu-item" @click="openThemePicker(); isHeaderMenuOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="13.5" cy="6.5" r=".5" fill="currentColor"></circle>
                      <circle cx="17.5" cy="10.5" r=".5" fill="currentColor"></circle>
                      <circle cx="8.5" cy="7.5" r=".5" fill="currentColor"></circle>
                      <circle cx="6.5" cy="12.5" r=".5" fill="currentColor"></circle>
                      <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.555-2.503 5.555-5.554C21.965 6.012 17.461 2 12 2z"></path>
                    </svg>
                    <span>Màu chủ đề đoạn chat</span>
                  </button>

                  <!-- 2. Tin nhắn đã lưu -->
                  <button type="button" class="header-menu-item" @click="openSavedMessagesModal(); isHeaderMenuOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" :fill="chatStore.savedMessages.length > 0 ? '#f59e0b' : 'none'" stroke="currentColor" stroke-width="2">
                      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                    </svg>
                    <span>Tin nhắn đã lưu ({{ chatStore.savedMessages.length }})</span>
                  </button>

                  <!-- 3. Đặt biệt danh (khi chat 1-1 hoặc nhóm) -->
                  <button 
                    v-if="chatStore.activeChatType !== 'PUBLIC'"
                    type="button" 
                    class="header-menu-item" 
                    @click="openNicknameModal(); isHeaderMenuOpen = false"
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
                      <line x1="7" y1="7" x2="7.01" y2="7"></line>
                    </svg>
                    <span>Đặt biệt danh</span>
                  </button>

                  <!-- 4. Quản lý thành viên nhóm (nếu là nhóm) -->
                  <button 
                    v-if="chatStore.activeChatType === 'GROUP'"
                    type="button" 
                    class="header-menu-item" 
                    @click="openGroupMembersModal(); isHeaderMenuOpen = false"
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                      <circle cx="9" cy="7" r="4"></circle>
                      <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                      <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                    <span>Thành viên nhóm ({{ chatStore.activeGroup?.memberCount }})</span>
                  </button>

                  <!-- Cài đặt nhạc chuông cuộc gọi -->
                  <button type="button" class="header-menu-item" @click="showRingtoneModal = true; isHeaderMenuOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M9 18V5l12-2v13"></path>
                      <circle cx="6" cy="18" r="3"></circle>
                      <circle cx="18" cy="16" r="3"></circle>
                    </svg>
                    <span>Nhạc chuông cuộc gọi 🎵</span>
                  </button>

                  <div class="dropdown-menu-divider"></div>

                  <!-- 5. Âm thanh thông báo -->
                  <button type="button" class="header-menu-item" @click="handleToggleSound">
                    <svg v-if="chatStore.isSoundEnabled" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                      <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                      <line x1="23" y1="9" x2="17" y2="15"></line>
                      <line x1="17" y1="9" x2="23" y2="15"></line>
                    </svg>
                    <span>Âm thanh: {{ chatStore.isSoundEnabled ? 'BẬT' : 'TẮT' }}</span>
                  </button>

                  <!-- 6. Thông báo Desktop -->
                  <button type="button" class="header-menu-item" @click="handleToggleNotification">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                      <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                    </svg>
                    <span>Thông báo đẩy: {{ chatStore.isNotificationEnabled ? 'BẬT' : 'TẮT' }}</span>
                  </button>

                  <!-- 7. Tải lại lịch sử chat -->
                  <button type="button" class="header-menu-item" @click="refreshCurrentHistory(); isHeaderMenuOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    <span>Tải lại tin nhắn</span>
                  </button>
                </div>
              </transition>
            </div>
          </div>
        </header>

        <!-- THANH TÌM KIẾM TIN NHẮN TRONG PHÒNG (IN-CHAT SEARCH) -->
        <transition name="search-expand">
          <div v-if="isSearchOpen" class="in-chat-search-bar">
            <div class="search-input-box">
              <svg class="search-icon-svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input 
                ref="searchInputRef"
                v-model="searchQuery" 
                type="text" 
                class="search-text-input" 
                placeholder="Tìm tin nhắn trong cuộc trò chuyện này... (Enter để tìm tiếp, ESC để đóng)"
                @keydown.enter.prevent="handleSearchNext"
                @keydown.esc="closeSearch"
              />
              <span v-if="searchQuery.trim()" class="search-counter-badge" :class="{ 'has-matches': searchMatchCount > 0 }">
                {{ searchMatchCount > 0 ? `${currentMatchIndex + 1}/${searchMatchCount}` : '0 kết quả' }}
              </span>
            </div>
            <div class="search-nav-controls">
              <button 
                class="btn-search-step" 
                :disabled="searchMatchCount === 0" 
                title="Kết quả trước (Shift+Enter)" 
                @click="handleSearchPrev"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="18 15 12 9 6 15"></polyline>
                </svg>
              </button>
              <button 
                class="btn-search-step" 
                :disabled="searchMatchCount === 0" 
                title="Kết quả tiếp theo (Enter)" 
                @click="handleSearchNext"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <button class="btn-search-exit" title="Đóng tìm kiếm (ESC)" @click="closeSearch">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>
        </transition>

        <!-- THANH TIN NHẮN ĐÃ GHIM (PINNED MESSAGE BANNER) -->
        <transition name="pinned-fade">
          <div v-if="currentPinnedList && currentPinnedList.length > 0" class="pinned-message-banner">
            <div class="pinned-banner-main" @click="scrollToPinnedMessage">
              <div class="pinned-icon-badge">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor">
                  <path d="M16 4l1 1v2l-3 3v5l-2 2-2-2V10L7 7V5l1-1h8m0-2H8c-1.1 0-2 .9-2 2v2.59L8.41 9H8v5.59L6 16.59V18h5v5l1 1 1-1v-5h5v-1.41l-2-2V9h-.41L18 6.59V4c0-1.1-.9-2-2-2z"/>
                </svg>
              </div>
              <div class="pinned-text-column">
                <div class="pinned-author-line">
                  <span class="pinned-badge-title">Tin nhắn đã ghim</span>
                  <span v-if="currentPinnedList.length > 1" class="pinned-counter-tag">{{ activePinnedIndex + 1 }}/{{ currentPinnedList.length }}</span>
                  <span class="pinned-author-name">• {{ chatStore.getNickname(currentActivePinnedMessage.senderUsername, currentActivePinnedMessage.senderFullName || currentActivePinnedMessage.senderUsername) }}</span>
                </div>
                <div class="pinned-preview-snippet">
                  {{ getPinnedSnippet(currentActivePinnedMessage) }}
                </div>
              </div>
            </div>
            <div class="pinned-banner-actions">
              <button 
                v-if="currentPinnedList.length > 1" 
                class="btn-pinned-action" 
                title="Xem tin ghim tiếp theo" 
                @click.stop="cyclePinnedMessage"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
              </button>
              <button 
                class="btn-pinned-action btn-unpin-trigger" 
                title="Bỏ ghim tin nhắn này" 
                @click.stop="handleUnpinMessage(currentActivePinnedMessage.id)"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>
        </transition>

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
                    'is-file-bubble': msg.type === 'FILE',
                    'is-deleted-bubble': msg.isDeleted,
                    'is-pinned-bubble': msg.isPinned,
                    'is-search-matched': highlightedMessageId === msg.id
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

                    <!-- Trường hợp: Tin nhắn Tệp tin / Tài liệu (FILE) -->
                    <div v-else-if="msg.type === 'FILE'" class="bubble-file-wrapper">
                      <div class="file-card-box">
                        <div class="file-icon-square" :class="getFileThemeClass(getFileExt(msg.content))">
                          <svg v-if="isPdf(msg.content)" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                            <polyline points="14 2 14 8 20 8"></polyline>
                            <line x1="16" y1="13" x2="8" y2="13"></line>
                            <line x1="16" y1="17" x2="8" y2="17"></line>
                          </svg>
                          <svg v-else-if="isZip(msg.content)" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-3"></path>
                            <line x1="10" y1="3" x2="10" y2="12"></line>
                            <line x1="10" y1="7" x2="14" y2="7"></line>
                            <line x1="10" y1="10" x2="14" y2="10"></line>
                          </svg>
                          <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                            <polyline points="13 2 13 9 20 9"></polyline>
                          </svg>
                        </div>
                        <div class="file-details">
                          <div class="file-card-title" :title="getFileName(msg.content)">{{ getFileName(msg.content) }}</div>
                          <div class="file-card-badges">
                            <span class="file-ext-chip">{{ getFileExt(msg.content).toUpperCase() }}</span>
                            <span v-if="getFileSize(msg.content)" class="file-size-chip">{{ formatFileSize(getFileSize(msg.content)) }}</span>
                          </div>
                        </div>
                        <a 
                          :href="getFileUrl(msg.content)" 
                          target="_blank" 
                          rel="noopener noreferrer"
                          :download="getFileName(msg.content)"
                          class="btn-file-dl" 
                          title="Tải về tệp tin"
                          @click.stop
                        >
                          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                            <polyline points="7 10 12 15 17 10"></polyline>
                            <line x1="12" y1="15" x2="12" y2="3"></line>
                          </svg>
                        </a>
                      </div>
                    </div>

                    <!-- Trường hợp: Tin nhắn thoại (VOICE) -->
                    <div v-else-if="msg.type === 'VOICE'" class="bubble-voice-wrapper">
                      <button type="button" class="btn-voice-play" @click.stop="togglePlayVoice(msg)">
                        <svg v-if="playingVoiceMsgId === msg.id" viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                          <rect x="6" y="4" width="4" height="16"></rect>
                          <rect x="14" y="4" width="4" height="16"></rect>
                        </svg>
                        <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                          <polygon points="5 3 19 12 5 21 5 3"></polygon>
                        </svg>
                      </button>
                      <div class="voice-waveform-box">
                        <div class="voice-bars">
                          <span v-for="n in 18" :key="n" class="waveform-bar" :class="{ 'is-animating': playingVoiceMsgId === msg.id }"></span>
                        </div>
                        <div class="voice-duration">
                          {{ playingVoiceMsgId === msg.id ? formatVoiceTime(currentVoiceProgress) : formatVoiceTime(getVoiceDuration(msg.content)) }}
                        </div>
                      </div>
                    </div>

                    <!-- Trường hợp: Cuộc bình chọn / Thăm dò ý kiến (POLL) -->
                    <div v-else-if="msg.type === 'POLL'" class="bubble-poll-wrapper">
                      <div class="poll-header">
                        <div class="poll-icon-box">📊</div>
                        <div class="poll-title-col">
                          <h5 class="poll-question">{{ getPollData(msg.content).question }}</h5>
                          <span class="poll-type-badge">{{ getPollData(msg.content).isMultiple ? 'Nhiều lựa chọn' : 'Một lựa chọn' }}</span>
                        </div>
                      </div>
                      <div class="poll-options-list">
                        <div 
                          v-for="opt in (getPollData(msg.content).options || [])" 
                          :key="opt.id"
                          class="poll-option-row"
                          :class="{ 'has-voted': (opt.votes || []).includes(authStore.user?.username) }"
                          @click.stop="handleVotePoll(msg.id, opt.id)"
                        >
                          <div class="poll-option-progress-bg" :style="{ width: getVotePercent(getPollData(msg.content), opt) + '%' }"></div>
                          <div class="poll-option-content">
                            <div class="poll-check-icon">
                              <svg v-if="(opt.votes || []).includes(authStore.user?.username)" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3">
                                <polyline points="20 6 9 17 4 12"></polyline>
                              </svg>
                              <span v-else class="poll-empty-circle"></span>
                            </div>
                            <span class="poll-option-text">{{ opt.text }}</span>
                            <span class="poll-vote-count">{{ (opt.votes || []).length }} ({{ getVotePercent(getPollData(msg.content), opt) }}%)</span>
                          </div>
                        </div>
                      </div>
                      <div class="poll-footer">
                        <span class="poll-total-votes">{{ getTotalVotes(getPollData(msg.content)) }} người đã bình chọn</span>
                      </div>
                    </div>

                    <!-- Trường hợp: Tin nhắn văn bản thông thường -->
                    <div v-else class="bubble-text" v-html="formatMessageWithMentions(msg.content)"></div>
                  </template>
                  
                  <!-- Thời gian & trạng thái gửi -->
                  <div class="bubble-meta">
                    <span v-if="msg.isPinned" class="meta-pinned-tag" title="Tin nhắn đã ghim">
                      <svg viewBox="0 0 24 24" width="11" height="11" fill="currentColor">
                        <path d="M16 4l1 1v2l-3 3v5l-2 2-2-2V10L7 7V5l1-1h8m0-2H8c-1.1 0-2 .9-2 2v2.59L8.41 9H8v5.59L6 16.59V18h5v5l1 1 1-1v-5h5v-1.41l-2-2V9h-.41L18 6.59V4c0-1.1-.9-2-2-2z"/>
                      </svg>
                      Đã ghim
                    </span>
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

                <!-- Thanh thao tác nhanh khi Hover: 6 animated emojis + Nút 3 chấm gọn gàng -->
                <div v-if="!msg.isDeleted" class="message-actions-hover">
                  <div class="hover-reactions-group">
                    <button 
                      v-for="emoji in ['👍', '❤️', '😂', '😮', '😢', '🔥']" 
                      :key="emoji"
                      type="button" 
                      class="btn-hover-reaction" 
                      :class="'emoji-' + getEmojiClass(emoji)"
                      :title="`Thả ${emoji}`"
                      @click.stop="chatStore.toggleReaction(msg.id, emoji)"
                    >
                      <span class="animated-emoji-char">{{ emoji }}</span>
                    </button>
                  </div>

                  <!-- Nút menu 3 chấm mở rộng -->
                  <div class="msg-more-menu-container">
                    <button 
                      type="button" 
                      class="btn-hover-more"
                      :class="{ 'is-active': activeMenuMessageId === msg.id }"
                      title="Thao tác khác"
                      @click.stop="toggleMessageMenu(msg.id, $event)"
                    >
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                        <circle cx="12" cy="12" r="2"></circle>
                        <circle cx="19" cy="12" r="2"></circle>
                        <circle cx="5" cy="12" r="2"></circle>
                      </svg>
                    </button>

                    <!-- Dropdown menu phân trang gọn gàng: Tránh che khuất & tràn màn hình -->
                    <div 
                      v-if="activeMenuMessageId === msg.id" 
                      :class="['msg-dropdown-menu', 'is-' + messageMenuDirection]" 
                      @click.stop
                    >
                      <!-- TRANG 1: Các thao tác thường dùng nhất (3-4 mục) -->
                      <div v-if="messageMenuPage === 1" class="menu-page-pane">
                        <button type="button" class="dropdown-item" @click="handleStartReply(msg); closeMessageMenu()">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="9 14 4 9 9 4"></polyline>
                            <path d="M20 20v-7a4 4 0 0 0-4-4H4"></path>
                          </svg>
                          <span>Trả lời</span>
                        </button>

                        <button v-if="msg.type === 'CHAT'" type="button" class="dropdown-item" @click="copyText(msg.content); closeMessageMenu()">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                          </svg>
                          <span>Sao chép</span>
                        </button>

                        <button type="button" class="dropdown-item" @click="handleBookmark(msg); closeMessageMenu()">
                          <svg viewBox="0 0 24 24" width="14" height="14" :fill="chatStore.isBookmarked(msg.id) ? '#f59e0b' : 'none'" stroke="currentColor" stroke-width="2">
                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                          </svg>
                          <span>{{ chatStore.isBookmarked(msg.id) ? 'Bỏ lưu ⭐' : 'Lưu tin nhắn ⭐' }}</span>
                        </button>

                        <!-- Xóa ở phía tôi (ẩn trên màn hình của mình) -->
                        <button type="button" class="dropdown-item is-danger-item" @click="handleDeleteForMe(msg); closeMessageMenu()">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                          </svg>
                          <span>Xóa ở phía tôi</span>
                        </button>

                        <div class="dropdown-menu-divider"></div>

                        <!-- Nút chuyển sang trang 2 -->
                        <button type="button" class="dropdown-item is-more-step" @click.stop="messageMenuPage = 2">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="1"></circle>
                            <circle cx="19" cy="12" r="1"></circle>
                            <circle cx="5" cy="12" r="1"></circle>
                          </svg>
                          <span>Xem thêm...</span>
                          <svg class="chevron-step" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="9 18 15 12 9 6"></polyline>
                          </svg>
                        </button>
                      </div>

                      <!-- TRANG 2: Thao tác nâng cao (Ghim, Sửa, Thu hồi) -->
                      <div v-else class="menu-page-pane">
                        <button type="button" class="dropdown-item is-back-step" @click.stop="messageMenuPage = 1">
                          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="15 18 9 12 15 6"></polyline>
                          </svg>
                          <span>Quay lại</span>
                        </button>

                        <button type="button" class="dropdown-item" @click="handleTogglePin(msg); closeMessageMenu()">
                          <svg viewBox="0 0 24 24" width="14" height="14" :fill="msg.isPinned ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                            <path d="M16 4l1 1v2l-3 3v5l-2 2-2-2V10L7 7V5l1-1h8m0-2H8c-1.1 0-2 .9-2 2v2.59L8.41 9H8v5.59L6 16.59V18h5v5l1 1 1-1v-5h5v-1.41l-2-2V9h-.41L18 6.59V4c0-1.1-.9-2-2-2z"/>
                          </svg>
                          <span>{{ msg.isPinned ? 'Bỏ ghim' : 'Ghim tin nhắn' }}</span>
                        </button>

                        <button v-if="isMyMessage(msg) && msg.type === 'CHAT'" type="button" class="dropdown-item" @click="handleStartEdit(msg); closeMessageMenu()">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                          </svg>
                          <span>Chỉnh sửa</span>
                        </button>

                        <button v-if="isMyMessage(msg) || isCurrentUserGroupAdmin" type="button" class="dropdown-item is-danger" @click="handleDeleteMessage(msg); closeMessageMenu()">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                          </svg>
                          <span>Thu hồi cho tất cả</span>
                        </button>
                      </div>
                    </div>
                  </div>
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

          <!-- POPUP AUTOCOMPLETE MENTION / NHẮC TÊN THÀNH VIÊN (@username) -->
          <transition name="mention-popup">
            <div v-if="mentionState.isOpen && filteredMentionMembers.length > 0" class="mention-autocomplete-card">
              <div class="mention-card-header">
                <span class="mention-card-title">Nhắc tên đồng nghiệp</span>
                <span class="mention-kbd-guide">↑↓ chọn • Enter chèn</span>
              </div>
              <ul class="mention-items-list">
                <li 
                  v-for="(member, idx) in filteredMentionMembers" 
                  :key="member.username"
                  class="mention-choice-row"
                  :class="{ 'is-focused': idx === mentionState.selectedIndex }"
                  @mouseenter="mentionState.selectedIndex = idx"
                  @mousedown.prevent="selectMentionMember(member)"
                >
                  <div class="mention-avatar-circle" :style="getAvatarBg(member.fullName || member.username)">
                    <img v-if="member.avatarUrl" :src="member.avatarUrl" class="mention-avatar-image" />
                    <span v-else>{{ getAvatarInitials(member.fullName || member.username) }}</span>
                  </div>
                  <div class="mention-user-details">
                    <span class="mention-name-text">{{ member.fullName || member.username }}</span>
                    <span class="mention-handle-text">@{{ member.username }}</span>
                  </div>
                </li>
              </ul>
            </div>
          </transition>

          <!-- Input file ẩn để đính kèm hình ảnh, video hoặc tài liệu -->
          <input 
            ref="imageFileInputRef" 
            type="file" 
            accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.rar,.7z,.txt,.csv" 
            class="hidden-file-input" 
            @change="handleMediaSelected"
          />

          <!-- HỘP SOẠN THẢO HỢP NHẤT HIỆN ĐẠI (UNIFIED MODERN CHAT COMPOSER) -->
          <form class="chat-composer-container" @submit.prevent="handleSendMessage">
            <!-- Giao diện đang ghi âm thoại -->
            <div v-if="isRecordingVoice" class="voice-recording-inline-bar">
              <span class="recording-pulse-dot"></span>
              <span class="recording-timer-text">{{ formatVoiceTimer(recordingSeconds) }}</span>
              <div class="recording-waves-animation">
                <span v-for="w in 14" :key="w" class="wave-stick"></span>
              </div>
              <span class="recording-status-msg">Đang thu âm tin nhắn thoại...</span>
              <div class="recording-btn-group">
                <button type="button" class="btn-cancel-recording" title="Hủy bỏ ghi âm" @click="cancelVoiceRecording">
                  ✕ Hủy
                </button>
                <button type="button" class="btn-send-recording" title="Hoàn tất và gửi" @click="finishAndSendVoice">
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="22" y1="2" x2="11" y2="13"></line>
                    <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                  </svg>
                  <span>Gửi</span>
                </button>
              </div>
            </div>

            <!-- Khung soạn thảo tin nhắn hợp nhất -->
            <div v-else class="composer-unified-card" :class="{ 'is-focused': isInputFocused }">
              <!-- Ô nhập văn bản -->
              <textarea
                ref="inputTextareaRef"
                v-model="inputContent"
                class="composer-textarea"
                :placeholder="editingMessage ? 'Nhập nội dung chỉnh sửa... (Enter lưu, ESC hủy)' : 'Nhập tin nhắn... (Enter gửi, Shift+Enter xuống dòng)'"
                rows="1"
                @focus="isInputFocused = true"
                @blur="isInputFocused = false"
                @keydown="handleTextareaKeydown"
                @input="handleInputWithMention"
                @paste="handlePaste"
              ></textarea>

              <!-- Thanh công cụ bên trong đáy hộp soạn thảo -->
              <div class="composer-bottom-toolbar">
                <div class="composer-tools-left">
                  <!-- Nút đính kèm ảnh, video hoặc tài liệu -->
                  <button 
                    type="button" 
                    class="composer-tool-btn" 
                    :class="{ 'is-uploading': isUploadingImage }"
                    :disabled="isUploadingImage || !chatStore.isConnected || isRecordingVoice"
                    :title="isUploadingImage ? 'Đang tải tệp lên Cloud...' : 'Đính kèm tệp tin, hình ảnh hoặc video (Ctrl+V)'"
                    @click="triggerImageSelect"
                  >
                    <svg v-if="!isUploadingImage" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
                    </svg>
                    <span v-else class="upload-spinner-icon"></span>
                  </button>

                  <!-- Nút Ghi âm Tin nhắn thoại (Voice note) -->
                  <button 
                    type="button" 
                    class="composer-tool-btn" 
                    :disabled="!chatStore.isConnected || isUploadingVoice"
                    title="Ghi âm tin nhắn thoại"
                    @click="toggleVoiceRecording"
                  >
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                      <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                      <line x1="12" y1="19" x2="12" y2="23"></line>
                      <line x1="8" y1="23" x2="16" y2="23"></line>
                    </svg>
                  </button>

                  <!-- Nút Tạo bình chọn (Poll) -->
                  <button 
                    type="button" 
                    class="composer-tool-btn" 
                    :disabled="!chatStore.isConnected"
                    title="Tạo cuộc thăm dò ý kiến / bình chọn"
                    @click="openCreatePollDialog"
                  >
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="20" x2="18" y2="10"></line>
                      <line x1="12" y1="20" x2="12" y2="4"></line>
                      <line x1="6" y1="20" x2="6" y2="14"></line>
                    </svg>
                  </button>

                  <!-- Nút Chia sẻ Thẻ Sản phẩm ERP -->
                  <button 
                    type="button" 
                    class="composer-tool-btn" 
                    :disabled="!chatStore.isConnected"
                    title="Chia sẻ Thẻ Sản phẩm ERP vào cuộc trò chuyện"
                    @click="openProductShareModal"
                  >
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                      <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                      <line x1="12" y1="22.08" x2="12" y2="12"></line>
                    </svg>
                  </button>

                  <div class="composer-tool-divider"></div>

                  <!-- Nút biểu tượng cảm xúc (Quick Emoji Popover) -->
                  <div class="composer-emoji-wrapper">
                    <button 
                      type="button" 
                      class="composer-tool-btn" 
                      :class="{ 'is-active': showEmojiPopover }"
                      title="Chèn biểu tượng cảm xúc nhanh"
                      @click="showEmojiPopover = !showEmojiPopover"
                    >
                      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
                        <line x1="9" y1="9" x2="9.01" y2="9"></line>
                        <line x1="15" y1="9" x2="15.01" y2="9"></line>
                      </svg>
                    </button>

                    <!-- Popover chèn emoji nhanh (gọn gàng, thanh lịch) -->
                    <transition name="popover-fade">
                      <div v-if="showEmojiPopover" class="composer-emoji-popover">
                        <div class="emoji-popover-header">
                          <span>Biểu tượng nhanh</span>
                          <button type="button" class="btn-close-emoji-popover" @click="showEmojiPopover = false">✕</button>
                        </div>
                        <div class="emoji-popover-grid">
                          <button 
                            v-for="emoji in quickEmojis" 
                            :key="emoji" 
                            type="button" 
                            class="btn-popover-emoji"
                            @click="insertEmoji(emoji)"
                          >
                            {{ emoji }}
                          </button>
                        </div>
                      </div>
                    </transition>
                  </div>
                </div>

                <!-- Nút gửi bên phải -->
                <div class="composer-tools-right">
                  <button 
                    type="submit" 
                    class="btn-send-message"
                    :disabled="!inputContent.trim() || !chatStore.isConnected"
                  >
                    <span>{{ editingMessage ? 'Lưu' : 'Gửi' }}</span>
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5">
                      <line x1="22" y1="2" x2="11" y2="13"></line>
                      <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </form>
        </footer>

      </main>

      <!-- CỘT PHẢI THỨ 3 (SLIDING RIGHT DRAWER): TÀI NGUYÊN MEDIA, TỆP TIN & LIÊN KẾT -->
      <transition name="drawer-slide">
        <aside v-if="showMediaDrawer" class="chat-right-drawer">
          <div class="drawer-header">
            <div class="drawer-title-group">
              <h4 class="drawer-title">Tài nguyên phòng chat</h4>
              <span class="drawer-subtitle">{{ activeTitle }}</span>
            </div>
            <button type="button" class="btn-close-drawer" title="Đóng ngăn lưu trữ" @click="showMediaDrawer = false">✕</button>
          </div>

          <div class="drawer-nav-tabs">
            <button 
              type="button" 
              class="drawer-tab" 
              :class="{ 'is-active': mediaActiveTab === 'media' }"
              @click="mediaActiveTab = 'media'"
            >
              Media ({{ currentRoomMediaList.length }})
            </button>
            <button 
              type="button" 
              class="drawer-tab" 
              :class="{ 'is-active': mediaActiveTab === 'files' }"
              @click="mediaActiveTab = 'files'"
            >
              Tệp ({{ currentRoomFilesList.length }})
            </button>
            <button 
              type="button" 
              class="drawer-tab" 
              :class="{ 'is-active': mediaActiveTab === 'links' }"
              @click="mediaActiveTab = 'links'"
            >
              Link ({{ currentRoomLinksList.length }})
            </button>
            <button 
              type="button" 
              class="drawer-tab" 
              :class="{ 'is-active': mediaActiveTab === 'pinned' }"
              @click="mediaActiveTab = 'pinned'"
            >
              Ghim ({{ currentPinnedList.length }})
            </button>
          </div>

          <div class="drawer-content-scroll">
            <!-- TAB 1: MEDIA (IMAGES & VIDEOS) -->
            <div v-if="mediaActiveTab === 'media'" class="drawer-media-grid">
              <div v-if="currentRoomMediaList.length === 0" class="drawer-empty-hint">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#94a3b8" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                  <circle cx="8.5" cy="8.5" r="1.5"></circle>
                  <polyline points="21 15 16 10 5 21"></polyline>
                </svg>
                <p>Chưa có hình ảnh hoặc video nào</p>
              </div>
              <div 
                v-for="m in currentRoomMediaList" 
                :key="m.id" 
                class="drawer-media-cell"
                @click="m.type === 'IMAGE' ? openImageLightbox(m.content) : null"
              >
                <img v-if="m.type === 'IMAGE'" :src="m.content" class="drawer-thumb-img" loading="lazy" />
                <video v-else-if="m.type === 'VIDEO'" :src="m.content" class="drawer-thumb-video"></video>
                <span v-if="m.type === 'VIDEO'" class="video-play-indicator">▶</span>
              </div>
            </div>

            <!-- TAB 2: TỆP TIN & TÀI LIỆU -->
            <div v-else-if="mediaActiveTab === 'files'" class="drawer-files-list">
              <div v-if="currentRoomFilesList.length === 0" class="drawer-empty-hint">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#94a3b8" stroke-width="1.5">
                  <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                  <polyline points="13 2 13 9 20 9"></polyline>
                </svg>
                <p>Chưa có tệp tài liệu nào được gửi</p>
              </div>
              <a 
                v-for="f in currentRoomFilesList" 
                :key="f.id"
                :href="getFileUrl(f.content)"
                target="_blank"
                rel="noopener noreferrer"
                :download="getFileName(f.content)"
                class="drawer-file-item"
              >
                <div class="drawer-file-icon-box">📎</div>
                <div class="drawer-file-text-col">
                  <span class="drawer-file-name" :title="getFileName(f.content)">{{ getFileName(f.content) }}</span>
                  <span class="drawer-file-meta">{{ formatSidebarTime(f.createdAt) }} • {{ formatFileSize(getFileSize(f.content)) }}</span>
                </div>
              </a>
            </div>

            <!-- TAB 3: LIÊN KẾT (LINKS) -->
            <div v-else-if="mediaActiveTab === 'links'" class="drawer-links-list">
              <div v-if="currentRoomLinksList.length === 0" class="drawer-empty-hint">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#94a3b8" stroke-width="1.5">
                  <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
                  <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                </svg>
                <p>Chưa có liên kết trang web nào</p>
              </div>
              <a 
                v-for="(link, lIdx) in currentRoomLinksList" 
                :key="lIdx"
                :href="link.url"
                target="_blank"
                rel="noopener noreferrer"
                class="drawer-link-item"
              >
                <div class="drawer-link-icon-box">🔗</div>
                <div class="drawer-link-text-col">
                  <span class="drawer-link-url">{{ link.url }}</span>
                  <span class="drawer-link-meta">{{ link.sender }} • {{ formatSidebarTime(link.time) }}</span>
                </div>
              </a>
            </div>

            <!-- TAB 4: TIN NHẮN ĐÃ GHIM -->
            <div v-else-if="mediaActiveTab === 'pinned'" class="drawer-pinned-list">
              <div v-if="currentPinnedList.length === 0" class="drawer-empty-hint">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#94a3b8" stroke-width="1.5">
                  <path d="M16 4l1 1v2l-3 3v5l-2 2-2-2V10L7 7V5l1-1h8m0-2H8c-1.1 0-2 .9-2 2v2.59L8.41 9H8v5.59L6 16.59V18h5v5l1 1 1-1v-5h5v-1.41l-2-2V9h-.41L18 6.59V4c0-1.1-.9-2-2-2z"/>
                </svg>
                <p>Không có tin nhắn nào được ghim trong phòng này</p>
              </div>
              <div 
                v-for="p in currentPinnedList" 
                :key="p.id"
                class="drawer-pinned-item"
                @click="scrollToMessage(p.id)"
              >
                <div class="drawer-pinned-header">
                  <span class="drawer-pinned-author">{{ chatStore.getNickname(p.senderUsername, p.senderFullName || p.senderUsername) }}</span>
                  <span class="drawer-pinned-time">{{ formatSidebarTime(p.createdAt) }}</span>
                </div>
                <p class="drawer-pinned-snippet">{{ getPinnedSnippet(p) }}</p>
              </div>
            </div>
          </div>
        </aside>
      </transition>
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

    <!-- MODAL TẠO CUỘC BÌNH CHỌN (POLL CREATION) -->
    <Teleport to="body">
      <div v-if="showCreatePollDialog" class="modal-backdrop-overlay" @click.self="showCreatePollDialog = false">
        <div class="custom-modal-card">
          <div class="modal-card-header">
            <div class="header-icon-title">
              <div class="modal-header-icon">📊</div>
              <h3 class="modal-card-title">Tạo cuộc thăm dò ý kiến</h3>
            </div>
            <button type="button" class="btn-close-modal" @click="showCreatePollDialog = false">✕</button>
          </div>

          <div class="modal-card-body">
            <div class="form-group-field">
              <label class="field-label">Câu hỏi bình chọn <span class="required-star">*</span></label>
              <input 
                v-model="pollQuestion" 
                type="text" 
                class="form-text-input" 
                placeholder="Ví dụ: Lịch họp dự án tuần sau vào mấy giờ?"
              />
            </div>

            <div class="form-group-field">
              <label class="field-label">Các lựa chọn bình chọn <span class="required-star">*</span></label>
              <div class="poll-options-inputs">
                <div v-for="(opt, oIdx) in pollOptions" :key="oIdx" class="poll-input-row">
                  <span class="poll-opt-index">{{ oIdx + 1 }}.</span>
                  <input 
                    v-model="pollOptions[oIdx]" 
                    type="text" 
                    class="form-text-input" 
                    :placeholder="`Lựa chọn ${oIdx + 1}`"
                  />
                  <button 
                    v-if="pollOptions.length > 2" 
                    type="button" 
                    class="btn-remove-opt" 
                    title="Xóa lựa chọn"
                    @click="pollOptions.splice(oIdx, 1)"
                  >
                    ✕
                  </button>
                </div>
              </div>
              <button 
                v-if="pollOptions.length < 8" 
                type="button" 
                class="btn-add-poll-option" 
                @click="pollOptions.push('')"
              >
                + Thêm lựa chọn
              </button>
            </div>

            <div class="form-group-field">
              <label class="poll-checkbox-label">
                <input type="checkbox" v-model="pollIsMultiple" class="check-box-input" />
                <span>Cho phép chọn nhiều phương án</span>
              </label>
            </div>
          </div>

          <div class="modal-card-footer">
            <button type="button" class="btn-modal-cancel" @click="showCreatePollDialog = false">Hủy</button>
            <button 
              type="button" 
              class="btn-modal-primary" 
              :disabled="!isPollValid"
              @click="handleCreatePollSubmit"
            >
              Tạo bình chọn
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL TIN NHẮN ĐÃ LƯU (SAVED MESSAGES) -->
    <Teleport to="body">
      <div v-if="showSavedMessagesModal" class="modal-backdrop-overlay" @click.self="showSavedMessagesModal = false">
        <div class="custom-modal-card modal-large">
          <div class="modal-card-header">
            <div class="header-icon-title">
              <div class="modal-header-icon">⭐</div>
              <div class="modal-title-group">
                <h3 class="modal-card-title">Tin nhắn đã lưu của tôi</h3>
                <span class="modal-card-desc">Sổ tay cá nhân lưu trữ các thông tin, ghi chú quan trọng ({{ chatStore.savedMessages.length }} tin nhắn)</span>
              </div>
            </div>
            <button type="button" class="btn-close-modal" @click="showSavedMessagesModal = false">✕</button>
          </div>

          <div class="modal-card-body">
            <div v-if="chatStore.savedMessages.length === 0" class="saved-empty-box">
              <div class="empty-star-icon">⭐</div>
              <h4>Chưa có tin nhắn nào được lưu</h4>
              <p>Rê chuột vào bất kỳ tin nhắn nào và chọn menu ⋯ &gt; "Lưu tin nhắn ⭐" để đưa vào sổ tay cá nhân này.</p>
            </div>

            <div v-else class="saved-messages-list-scroll">
              <div v-for="sm in chatStore.savedMessages" :key="sm.id" class="saved-message-card">
                <div class="saved-card-top">
                  <span class="saved-author-name">{{ sm.senderFullName || sm.senderUsername }}</span>
                  <span class="saved-time-tag">{{ formatSidebarTime(sm.createdAt) }}</span>
                </div>
                <div class="saved-card-body">
                  <p v-if="sm.type === 'CHAT'" class="saved-content-text">{{ sm.content }}</p>
                  <span v-else-if="sm.type === 'IMAGE'" class="saved-type-tag">📷 [Hình ảnh]</span>
                  <span v-else-if="sm.type === 'VIDEO'" class="saved-type-tag">🎥 [Video]</span>
                  <span v-else-if="sm.type === 'FILE'" class="saved-type-tag">📎 [Tệp tin]</span>
                  <span v-else-if="sm.type === 'VOICE'" class="saved-type-tag">🎙️ [Tin nhắn thoại]</span>
                  <span v-else-if="sm.type === 'POLL'" class="saved-type-tag">📊 [Bình chọn]</span>
                </div>
                <div class="saved-card-actions">
                  <button type="button" class="btn-saved-action" title="Sao chép nội dung" @click="copyText(sm.content)">
                    📋 Sao chép
                  </button>
                  <button type="button" class="btn-saved-action is-remove" title="Xóa khỏi mục đã lưu" @click="chatStore.toggleBookmarkMessage(sm)">
                    🗑️ Bỏ lưu
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-card-footer">
            <button type="button" class="btn-modal-cancel" @click="showSavedMessagesModal = false">Đóng</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL CHỌN MÀU CHỦ ĐỀ ĐOẠN CHAT (CHAT THEME PICKER) -->
    <Teleport to="body">
      <div v-if="showThemeModal" class="modal-backdrop-overlay" @click.self="showThemeModal = false">
        <div class="custom-modal-card">
          <div class="modal-card-header">
            <div class="header-icon-title">
              <div class="modal-header-icon">🎨</div>
              <h3 class="modal-card-title">Đổi màu chủ đề cuộc trò chuyện</h3>
            </div>
            <button type="button" class="btn-close-modal" @click="showThemeModal = false">✕</button>
          </div>

          <div class="modal-card-body">
            <p class="theme-picker-desc">Màu sắc bong bóng tin nhắn gửi đi được lưu riêng cho từng cuộc trò chuyện trên thiết bị của bạn.</p>
            <div class="theme-presets-grid">
              <div 
                v-for="th in colorThemes" 
                :key="th.key"
                class="theme-preset-card"
                :class="{ 'is-selected': currentRoomThemeKey === th.key }"
                @click="changeTheme(th.key)"
              >
                <div class="theme-swatch-circle" :style="{ background: th.gradient }">
                  <svg v-if="currentRoomThemeKey === th.key" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ffffff" stroke-width="3">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                </div>
                <span class="theme-preset-name">{{ th.name }}</span>
              </div>
            </div>
          </div>

          <div class="modal-card-footer">
            <button type="button" class="btn-modal-primary" @click="showThemeModal = false">Hoàn tất</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ===================================================================== -->
    <!-- MODAL CUỘC GỌI WEBRTC (GỌI THOẠI & GỌI VIDEO NỘI BỘ)                -->
    <!-- ===================================================================== -->
    <Teleport to="body">
      <!-- 1. Hộp thoại khi có cuộc gọi đến (Incoming Call) -->
      <div 
        v-if="chatStore.callState.isOpen && chatStore.callState.isIncoming" 
        class="incoming-call-overlay"
      >
        <div class="incoming-call-card">
          <div class="incoming-avatar-sonar-wrapper">
            <span class="sonar-ring"></span>
            <span class="sonar-ring r2"></span>
            <span class="sonar-ring r3"></span>
            <div class="incoming-caller-avatar" :style="getAvatarBg(chatStore.callState.partnerFullName || chatStore.callState.partnerUsername)">
              <img v-if="chatStore.callState.partnerAvatarUrl" :src="chatStore.callState.partnerAvatarUrl" alt="avatar" />
              <span v-else>{{ getAvatarInitials(chatStore.callState.partnerFullName || chatStore.callState.partnerUsername) }}</span>
            </div>
          </div>

          <h3 class="incoming-caller-name">{{ chatStore.callState.partnerFullName || chatStore.callState.partnerUsername }}</h3>
          <p class="incoming-call-subtitle">
            Cuộc gọi {{ chatStore.callState.callType === 'video' ? 'video' : 'thoại' }} nội bộ đến...
          </p>

          <div class="incoming-call-actions">
            <!-- Nút từ chối -->
            <button type="button" class="btn-call-action btn-call-decline" @click="handleDeclineIncomingCall">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
              </svg>
              <span>Từ chối</span>
            </button>

            <!-- Nút chấp nhận -->
            <button type="button" class="btn-call-action btn-call-accept" @click="handleAcceptIncomingCall">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
              </svg>
              <span>Trả lời</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 2. Giao diện đang thực hiện cuộc gọi (Active Calling Stage) -->
      <div 
        v-if="chatStore.callState.isOpen && !chatStore.callState.isIncoming" 
        class="active-call-overlay"
      >
        <!-- Thẻ Audio ẩn để luôn phát âm thanh từ đối phương (cho cả gọi Thoại lẫn Video) -->
        <audio ref="remoteAudioRef" autoplay playsinline style="display: none;"></audio>

        <div class="active-call-stage">
          <!-- Call Stage Header -->
          <div class="call-stage-header">
            <div class="call-header-info">
              <h4>{{ chatStore.callState.partnerFullName || chatStore.callState.partnerUsername }}</h4>
              <div v-if="chatStore.callState.status === 'CONNECTED'" class="active-timer-pill">
                <span class="live-red-dot"></span>
                <span>{{ callDurationFormatted }}</span>
              </div>
              <span v-else class="pulse-call-text">Đang đổ chuông...</span>
            </div>
          </div>

          <!-- Khu vực hiển thị Video (khi gọi Video) -->
          <div v-if="chatStore.callState.callType === 'video'" class="call-video-grid">
            <div class="remote-video-container">
              <video 
                ref="remoteVideoRef" 
                autoplay 
                playsinline 
                muted
                class="remote-video-element"
              ></video>
              <!-- Placeholder khi chưa có remote stream -->
              <div v-if="!hasRemoteVideoTrack" class="video-placeholder-cover">
                <div class="large-avatar-call" :style="getAvatarBg(chatStore.callState.partnerFullName || chatStore.callState.partnerUsername)">
                  {{ getAvatarInitials(chatStore.callState.partnerFullName || chatStore.callState.partnerUsername) }}
                </div>
                <span class="wait-cam-text">Đang kết nối camera đối phương...</span>
              </div>
            </div>

            <!-- Khung hình PiP Video của chính mình -->
            <div class="local-pip-container">
              <video 
                ref="localVideoRef" 
                autoplay 
                playsinline 
                muted 
                class="local-video-element"
              ></video>
              <div v-if="isCameraOff" class="pip-cam-off-badge">
                <span>Camera đã tắt</span>
              </div>
            </div>
          </div>

          <!-- Khu vực hiển thị Âm thanh (khi gọi Thoại Audio) -->
          <div v-else class="call-audio-stage">
            <div class="audio-caller-avatar-circle" :style="getAvatarBg(chatStore.callState.partnerFullName || chatStore.callState.partnerUsername)">
              <img v-if="chatStore.callState.partnerAvatarUrl" :src="chatStore.callState.partnerAvatarUrl" alt="avatar" />
              <span v-else>{{ getAvatarInitials(chatStore.callState.partnerFullName || chatStore.callState.partnerUsername) }}</span>
              <div class="audio-soundwaves-ring">
                <span class="sound-wave"></span>
                <span class="sound-wave sw2"></span>
              </div>
            </div>
            <p class="audio-call-note">
              {{ chatStore.callState.status === 'CONNECTED' ? 'Đang trong cuộc gọi thoại bảo mật' : 'Đang kết nối tín hiệu cuộc gọi thoại...' }}
            </p>
          </div>

          <!-- Thanh điều khiển cuộc gọi dưới đáy -->
          <div class="call-controls-bar">
            <!-- Nút Bật/Tắt Micro -->
            <button 
              type="button" 
              class="btn-call-ctl" 
              :class="{ 'is-active-off': isMuted }"
              :title="isMuted ? 'Bật Micro' : 'Tắt Micro'"
              @click="toggleMuteAudio"
            >
              <svg v-if="!isMuted" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                <line x1="12" y1="19" x2="12" y2="23"></line>
                <line x1="8" y1="23" x2="16" y2="23"></line>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="1" y1="1" x2="23" y2="23"></line>
                <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"></path>
                <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23"></path>
                <line x1="12" y1="19" x2="12" y2="23"></line>
                <line x1="8" y1="23" x2="16" y2="23"></line>
              </svg>
            </button>

            <!-- Nút Bật/Tắt Camera (khi gọi Video) -->
            <button 
              v-if="chatStore.callState.callType === 'video'"
              type="button" 
              class="btn-call-ctl" 
              :class="{ 'is-active-off': isCameraOff }"
              :title="isCameraOff ? 'Bật Camera' : 'Tắt Camera'"
              @click="toggleCameraVideo"
            >
              <svg v-if="!isCameraOff" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="23 7 16 12 23 17 23 7"></polygon>
                <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="1" y1="1" x2="23" y2="23"></line>
                <path d="M21 21l-3.34-3.34M23 7l-7 5 7 5V7z"></path>
                <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
              </svg>
            </button>

            <!-- Nút Kết thúc cuộc gọi / Gác máy -->
            <button 
              type="button" 
              class="btn-call-ctl btn-hangup" 
              title="Kết thúc cuộc gọi"
              @click="hangUpCall"
            >
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 3. MODAL CÀI ĐẶT NHẠC CHUÔNG CUỘC GỌI -->
      <div v-if="showRingtoneModal" class="custom-modal-overlay" @click.self="stopRingtonePreview(); showRingtoneModal = false">
        <div class="custom-modal-card ringtone-modal-card">
          <div class="custom-modal-header">
            <div class="modal-header-title">
              <span class="modal-header-icon">🎵</span>
              <div>
                <h4>Cài đặt nhạc chuông cuộc gọi</h4>
                <p>Tùy chỉnh âm thanh thông báo khi có đồng nghiệp gọi đến</p>
              </div>
            </div>
            <button type="button" class="btn-close-modal" @click="stopRingtonePreview(); showRingtoneModal = false">✕</button>
          </div>

          <div class="custom-modal-body">
            <div class="ringtone-list">
              <div 
                v-for="rt in ringtoneOptions" 
                :key="rt.id"
                class="ringtone-item-row"
                :class="{ 'is-selected': userRingtone === rt.id }"
                @click="userRingtone = rt.id"
              >
                <div class="ringtone-radio-dot">
                  <span v-if="userRingtone === rt.id" class="radio-inner"></span>
                </div>
                <div class="ringtone-info">
                  <div class="ringtone-name">{{ rt.name }}</div>
                  <div class="ringtone-desc">{{ rt.desc }}</div>
                </div>
                <button 
                  type="button" 
                  class="btn-preview-ringtone" 
                  :class="{ 'is-playing': previewingRingtone === rt.id }"
                  @click.stop="previewRingtone(rt.id)"
                  :title="previewingRingtone === rt.id ? 'Dừng phát' : 'Nghe thử nhạc chuông'"
                >
                  <span v-if="previewingRingtone === rt.id">⏹ Dừng</span>
                  <span v-else>▶ Nghe thử</span>
                </button>
              </div>
            </div>

            <!-- Âm lượng nhạc chuông -->
            <div class="ringtone-volume-row">
              <span class="volume-label">Âm lượng chuông:</span>
              <input 
                type="range" 
                min="0.1" 
                max="1" 
                step="0.05" 
                v-model.number="userRingtoneVolume" 
                class="ringtone-vol-slider"
              />
              <span class="volume-val">{{ Math.round(userRingtoneVolume * 100) }}%</span>
            </div>
          </div>

          <div class="custom-modal-footer">
            <button type="button" class="btn-cancel" @click="stopRingtonePreview(); showRingtoneModal = false">Đóng</button>
            <button type="button" class="btn-confirm-save" @click="saveRingtone(userRingtone)">Lưu cài đặt</button>
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

// State Tìm kiếm tin nhắn trong phòng (In-Chat Search)
const isSearchOpen = ref(false)
const searchQuery = ref('')
const currentMatchIndex = ref(0)
const searchInputRef = ref(null)
const highlightedMessageId = ref(null)

// State Ghim tin nhắn (Pinned Messages)
const activePinnedIndex = ref(0)

// State Nhắc tên thành viên (Mention / Tag)
const mentionState = ref({
  isOpen: false,
  query: '',
  selectedIndex: 0,
  atIndex: -1
})

// State đính kèm hình ảnh / video & Lightbox
const imageFileInputRef = ref(null)
const isUploadingImage = ref(false)
const previewImageUrl = ref(null)

// State Responsive Mobile
const isMobileScreen = ref(typeof window !== 'undefined' ? window.innerWidth <= 768 : false)
const showMobileChat = ref(typeof window !== 'undefined' ? window.innerWidth > 768 : true)
const backToSidebar = () => {
  showMobileChat.value = false
}
const handleResize = () => {
  isMobileScreen.value = window.innerWidth <= 768
}

// State Menu 3 chấm thao tác tin nhắn (Phân trang gọn gàng & vị trí thông minh)
const activeMenuMessageId = ref(null)
const messageMenuPage = ref(1)
const messageMenuDirection = ref('up')

const toggleMessageMenu = (id, event) => {
  if (activeMenuMessageId.value === id) {
    activeMenuMessageId.value = null
    messageMenuPage.value = 1
    return
  }
  activeMenuMessageId.value = id
  messageMenuPage.value = 1
  if (event?.currentTarget) {
    const rect = event.currentTarget.getBoundingClientRect()
    // Nếu nút gần cạnh trên màn hình (< 260px), xổ xuống dưới để không bị che khuất bởi header
    messageMenuDirection.value = rect.top < 260 ? 'down' : 'up'
  }
}

const closeMessageMenu = () => {
  activeMenuMessageId.value = null
  messageMenuPage.value = 1
}

// State Menu 3 chấm mở rộng trên Chat Header
const isHeaderMenuOpen = ref(false)

const closeAllFloatingMenus = () => {
  closeMessageMenu()
  isHeaderMenuOpen.value = false
}

// State Chủ đề màu sắc đoạn chat (Theme Color)
const showThemeModal = ref(false)
const openThemePicker = () => {
  showThemeModal.value = true
}

const colorThemes = [
  { key: 'blue', name: 'Xanh dương tiêu chuẩn', primary: '#2563eb', gradient: 'linear-gradient(135deg, #2563eb, #1d4ed8)' },
  { key: 'emerald', name: 'Xanh lục bảo', primary: '#059669', gradient: 'linear-gradient(135deg, #059669, #047857)' },
  { key: 'purple', name: 'Tím hoàng gia', primary: '#7c3aed', gradient: 'linear-gradient(135deg, #7c3aed, #6d28d9)' },
  { key: 'indigo', name: 'Chàm thời thượng', primary: '#4f46e5', gradient: 'linear-gradient(135deg, #6366f1, #4338ca)' },
  { key: 'cyan', name: 'Xanh ngọc biển', primary: '#0891b2', gradient: 'linear-gradient(135deg, #06b6d4, #0e7490)' },
  { key: 'teal', name: 'Xanh mòng két', primary: '#0d9488', gradient: 'linear-gradient(135deg, #14b8a6, #0f766e)' },
  { key: 'mint', name: 'Xanh bạc hà Mint', primary: '#10b981', gradient: 'linear-gradient(135deg, #34d399, #059669)' },
  { key: 'orange', name: 'Cam hoàng hôn', primary: '#ea580c', gradient: 'linear-gradient(135deg, #ea580c, #c2410c)' },
  { key: 'amber', name: 'Vàng hổ phách', primary: '#d97706', gradient: 'linear-gradient(135deg, #f59e0b, #b45309)' },
  { key: 'pink', name: 'Hồng ngọc', primary: '#e11d48', gradient: 'linear-gradient(135deg, #e11d48, #be123c)' },
  { key: 'rose', name: 'Cánh hồng Rose Gold', primary: '#f43f5e', gradient: 'linear-gradient(135deg, #fb7185, #e11d48)' },
  { key: 'violet', name: 'Tím hoa cà', primary: '#8b5cf6', gradient: 'linear-gradient(135deg, #a78bfa, #7c3aed)' },
  { key: 'cyberpunk', name: 'Neon Cyberpunk', primary: '#d946ef', gradient: 'linear-gradient(135deg, #ec4899, #8b5cf6)' },
  { key: 'sunset', name: 'Rực rỡ Sunset', primary: '#f97316', gradient: 'linear-gradient(135deg, #f43f5e, #f97316)' },
  { key: 'slate', name: 'Ghi than tối giản', primary: '#334155', gradient: 'linear-gradient(135deg, #475569, #1e293b)' }
]

const currentRoomThemeKey = ref(localStorage.getItem(`erp_chat_theme_${chatStore.activeRoomId}`) || 'blue')

watch(() => chatStore.activeRoomId, (newId) => {
  currentRoomThemeKey.value = localStorage.getItem(`erp_chat_theme_${newId}`) || 'blue'
})

const currentThemeObject = computed(() => {
  return colorThemes.find(t => t.key === currentRoomThemeKey.value) || colorThemes[0]
})

const chatMainStyle = computed(() => ({
  '--chat-theme-primary': currentThemeObject.value.primary,
  '--chat-theme-gradient': currentThemeObject.value.gradient
}))

const changeTheme = (key) => {
  currentRoomThemeKey.value = key
  localStorage.setItem(`erp_chat_theme_${chatStore.activeRoomId}`, key)
  ElMessage.success('Đã áp dụng chủ đề màu cho cuộc trò chuyện!')
}

// State Ngăn lưu trữ Media & Tài liệu (Right Drawer)
const showMediaDrawer = ref(false)
const mediaActiveTab = ref('media')

const currentRoomMediaList = computed(() => {
  return chatStore.currentMessages.filter(m => (m.type === 'IMAGE' || m.type === 'VIDEO') && !m.isDeleted)
})

const currentRoomFilesList = computed(() => {
  return chatStore.currentMessages.filter(m => m.type === 'FILE' && !m.isDeleted)
})

const currentRoomLinksList = computed(() => {
  const links = []
  const urlRegex = /(https?:\/\/[^\s]+)/g
  chatStore.currentMessages.forEach(m => {
    if (m.type === 'CHAT' && !m.isDeleted && m.content) {
      const matches = m.content.match(urlRegex)
      if (matches) {
        matches.forEach(url => {
          links.push({
            url,
            sender: chatStore.getNickname(m.senderUsername, m.senderFullName || m.senderUsername),
            time: m.createdAt
          })
        })
      }
    }
  })
  return links
})

// State Tin nhắn đã lưu (Saved/Bookmarks)
const showSavedMessagesModal = ref(false)
const openSavedMessagesModal = () => {
  showSavedMessagesModal.value = true
}

const handleBookmark = (msg) => {
  const saved = chatStore.toggleBookmarkMessage(msg)
  if (saved) {
    ElMessage.success('Đã lưu tin nhắn vào sổ tay ⭐')
  } else {
    ElMessage.info('Đã bỏ lưu tin nhắn')
  }
}

const copyText = (text) => {
  if (!text) return
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text)
    ElMessage.success('Đã sao chép nội dung!')
  }
}

// State Ghi âm Tin nhắn thoại (Voice Message)
const isRecordingVoice = ref(false)
const recordingSeconds = ref(0)
let recordingTimer = null
let mediaRecorder = null
let audioChunks = []
const isUploadingVoice = ref(false)

const startVoiceRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioChunks = []
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.onstop = () => {
      stream.getTracks().forEach(t => t.stop())
    }
    mediaRecorder.start()
    isRecordingVoice.value = true
    recordingSeconds.value = 0
    recordingTimer = setInterval(() => {
      recordingSeconds.value++
    }, 1000)
  } catch (err) {
    ElMessage.error('Không thể truy cập Microphone. Vui lòng cấp quyền ghi âm trên trình duyệt!')
  }
}

const cancelVoiceRecording = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  clearInterval(recordingTimer)
  isRecordingVoice.value = false
  audioChunks = []
  recordingSeconds.value = 0
}

const finishAndSendVoice = async () => {
  if (!mediaRecorder) return
  clearInterval(recordingTimer)
  const duration = recordingSeconds.value
  isRecordingVoice.value = false

  mediaRecorder.onstop = async () => {
    const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
    const file = new File([audioBlob], `voice_${Date.now()}.webm`, { type: 'audio/webm' })
    isUploadingVoice.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      const res = await api.post('/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      const audioUrl = res.data?.url
      if (audioUrl) {
        if (chatStore.activeChatType === 'PUBLIC' || chatStore.activeChatType === 'GROUP') {
          chatStore.sendPublicVoice(audioUrl, duration, chatStore.activeRoomId)
        } else if (chatStore.activePartner) {
          chatStore.sendPrivateVoice(chatStore.activePartner.username, audioUrl, duration)
        }
      }
    } catch (err) {
      ElMessage.error('Gửi tin nhắn thoại thất bại: ' + (err.response?.data?.message || err.message))
    } finally {
      isUploadingVoice.value = false
      audioChunks = []
      recordingSeconds.value = 0
    }
  }
  mediaRecorder.stop()
}

const toggleVoiceRecording = () => {
  if (isRecordingVoice.value) {
    finishAndSendVoice()
  } else {
    startVoiceRecording()
  }
}

const formatVoiceTimer = (secs) => {
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// Audio Player cho bong bóng thoại
const playingVoiceMsgId = ref(null)
const currentVoiceProgress = ref(0)
let currentAudio = null
let voiceProgressTimer = null

const togglePlayVoice = (msg) => {
  if (playingVoiceMsgId.value === msg.id) {
    if (currentAudio) {
      currentAudio.pause()
    }
    playingVoiceMsgId.value = null
    clearInterval(voiceProgressTimer)
    return
  }

  if (currentAudio) {
    currentAudio.pause()
    clearInterval(voiceProgressTimer)
  }

  let voiceUrl = ''
  try {
    const parsed = JSON.parse(msg.content)
    voiceUrl = parsed.url
  } catch (e) {
    voiceUrl = msg.content
  }

  if (!voiceUrl) return

  currentAudio = new Audio(voiceUrl)
  playingVoiceMsgId.value = msg.id
  currentVoiceProgress.value = 0

  currentAudio.play().catch(e => {
    ElMessage.error('Không thể phát tin nhắn thoại này!')
    playingVoiceMsgId.value = null
  })

  voiceProgressTimer = setInterval(() => {
    if (currentAudio) {
      currentVoiceProgress.value = Math.floor(currentAudio.currentTime)
    }
  }, 500)

  currentAudio.onended = () => {
    playingVoiceMsgId.value = null
    clearInterval(voiceProgressTimer)
    currentVoiceProgress.value = 0
  }
}

const getVoiceDuration = (content) => {
  try {
    const parsed = JSON.parse(content)
    return parsed.duration || 0
  } catch (e) {
    return 0
  }
}

const formatVoiceTime = (secs) => {
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

// State Thăm dò ý kiến / Bình chọn (Polls)
const showCreatePollDialog = ref(false)
const pollQuestion = ref('')
const pollOptions = ref(['', ''])
const pollIsMultiple = ref(false)

const isPollValid = computed(() => {
  const validOpts = pollOptions.value.filter(o => o.trim())
  return pollQuestion.value.trim().length > 0 && validOpts.length >= 2
})

const openCreatePollDialog = () => {
  pollQuestion.value = ''
  pollOptions.value = ['', '']
  pollIsMultiple.value = false
  showCreatePollDialog.value = true
}

const handleCreatePollSubmit = () => {
  if (!isPollValid.value) return
  const validOpts = pollOptions.value
    .map(o => o.trim())
    .filter(Boolean)
    .map((text, idx) => ({ id: idx + 1, text, votes: [] }))

  const pollData = {
    question: pollQuestion.value.trim(),
    options: validOpts,
    isMultiple: pollIsMultiple.value
  }

  if (chatStore.activeChatType === 'PUBLIC' || chatStore.activeChatType === 'GROUP') {
    chatStore.sendPublicPoll(pollData, chatStore.activeRoomId)
  } else if (chatStore.activePartner) {
    chatStore.sendPrivatePoll(chatStore.activePartner.username, pollData)
  }

  showCreatePollDialog.value = false
  nextTick(() => scrollToBottom(true))
}

const handleVotePoll = async (msgId, optId) => {
  try {
    await chatStore.votePoll(msgId, optId)
  } catch (err) {
    ElMessage.error('Bình chọn thất bại!')
  }
}

const getPollData = (content) => {
  try {
    if (typeof content === 'string' && content.startsWith('{')) {
      return JSON.parse(content)
    }
    return content || { question: 'Bình chọn', options: [] }
  } catch (e) {
    return { question: 'Bình chọn', options: [] }
  }
}

const getTotalVotes = (poll) => {
  if (!poll || !poll.options) return 0
  const uniqueUsers = new Set()
  poll.options.forEach(o => {
    (o.votes || []).forEach(u => uniqueUsers.add(u))
  })
  return uniqueUsers.size
}

const getVotePercent = (poll, opt) => {
  const total = getTotalVotes(poll)
  if (total === 0) return 0
  const votes = (opt.votes || []).length
  return Math.round((votes / total) * 100)
}

// Helper lấy tên hiển thị của đồng nghiệp theo biệt danh phòng riêng
const getColleagueDisplayName = (colleague) => {
  if (!colleague) return ''
  const myUsername = authStore.user?.username
  const privateRoomId = chatStore.getPrivateRoomId(myUsername, colleague.username)
  return chatStore.roomNicknames[privateRoomId]?.[colleague.username] 
    || colleague.nickname 
    || colleague.fullName 
    || colleague.username
}

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
    return `${fullName} (@${partnerUsername})`
  }
  return `@${partnerUsername || ''}`
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

// Tính năng Ghim cuộc trò chuyện (Pin room/conversation)
const pinnedRoomIds = ref(JSON.parse(localStorage.getItem('erp_pinned_rooms_' + (authStore.user?.username || 'user')) || '[]'))
const isRoomPinned = (id) => pinnedRoomIds.value.includes(id)
const togglePinRoom = (id) => {
  const idx = pinnedRoomIds.value.indexOf(id)
  if (idx !== -1) {
    pinnedRoomIds.value.splice(idx, 1)
  } else {
    pinnedRoomIds.value.push(id)
  }
  localStorage.setItem('erp_pinned_rooms_' + (authStore.user?.username || 'user'), JSON.stringify(pinnedRoomIds.value))
  ElMessage.success(idx === -1 ? 'Đã ghim cuộc trò chuyện lên đầu' : 'Đã bỏ ghim cuộc trò chuyện')
}

// Sắp xếp danh sách đồng nghiệp: Pinned lên đầu -> Tin chưa đọc -> Bình thường
const sortedFilteredColleagues = computed(() => {
  const list = [...filteredColleagues.value]
  return list.sort((a, b) => {
    const pA = isRoomPinned(a.username) ? 1 : 0
    const pB = isRoomPinned(b.username) ? 1 : 0
    if (pA !== pB) return pB - pA
    const uA = getPrivateUnreadCount(a.username) > 0 ? 1 : 0
    const uB = getPrivateUnreadCount(b.username) > 0 ? 1 : 0
    if (uA !== uB) return uB - uA
    return 0
  })
})

// Sắp xếp nhóm chat: Nhóm được ghim lên trước
const sortedMyGroups = computed(() => {
  const list = [...chatStore.myGroups]
  return list.sort((a, b) => {
    const pA = isRoomPinned('GROUP_' + a.id) ? 1 : 0
    const pB = isRoomPinned('GROUP_' + b.id) ? 1 : 0
    return pB - pA
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
  if (chatStore.unreadCounts[roomId] !== undefined && chatStore.unreadCounts[roomId] > 0) {
    return chatStore.unreadCounts[roomId]
  }
  const colleague = chatStore.colleagues.find(c => c.username?.toLowerCase() === partnerUsername?.toLowerCase())
  return colleague?.unreadCount || 0
}

// Đánh dấu tất cả tin nhắn đã đọc
const handleMarkAllRead = async () => {
  try {
    await chatStore.markAllAsRead()
    ElMessage.success('Đã đánh dấu tất cả tin nhắn là đã đọc')
  } catch (e) {
    ElMessage.error('Không thể đánh dấu đã đọc')
  }
}

// Chuyển sang phòng chung
const selectPublicRoom = async () => {
  showMobileChat.value = true
  await chatStore.selectRoom('PUBLIC')
  await nextTick()
  scrollToBottom(false)
  setTimeout(() => scrollToBottom(false), 50)
  setTimeout(() => scrollToBottom(false), 200)
  inputTextareaRef.value?.focus()
}

// Chuyển sang chat 1-1 với đồng nghiệp
const selectPrivatePartner = async (partner) => {
  showMobileChat.value = true
  partner.unreadCount = 0
  const myUsername = authStore.user?.username
  const roomId = chatStore.getPrivateRoomId(myUsername, partner.username)
  if (chatStore.unreadCounts[roomId]) {
    chatStore.totalUnreadCount = Math.max(0, chatStore.totalUnreadCount - chatStore.unreadCounts[roomId])
    chatStore.unreadCounts[roomId] = 0
  }
  await chatStore.selectRoom('PRIVATE', partner)
  await nextTick()
  scrollToBottom(false)
  setTimeout(() => scrollToBottom(false), 50)
  setTimeout(() => scrollToBottom(false), 200)
  inputTextareaRef.value?.focus()
}

// Chuyển sang phòng nhóm
const selectGroupRoom = async (group) => {
  showMobileChat.value = true
  const roomId = `GROUP_${group.id}`
  if (chatStore.unreadCounts[roomId]) {
    chatStore.totalUnreadCount = Math.max(0, chatStore.totalUnreadCount - chatStore.unreadCounts[roomId])
    chatStore.unreadCounts[roomId] = 0
  }
  await chatStore.selectRoom('GROUP', group)
  await nextTick()
  scrollToBottom(false)
  setTimeout(() => scrollToBottom(false), 50)
  setTimeout(() => scrollToBottom(false), 200)
  inputTextareaRef.value?.focus()
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

// Xóa tin nhắn ở phía tôi (đối phương vẫn thấy bình thường)
const handleDeleteForMe = async (msg) => {
  if (!msg) return
  try {
    await ElMessageBox.confirm(
      'Tin nhắn này sẽ bị ẩn khỏi màn hình trò chuyện của bạn. Đối phương vẫn sẽ nhìn thấy tin nhắn bình thường.',
      'Xóa tin nhắn ở phía tôi?',
      {
        confirmButtonText: 'Xóa ở phía tôi',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }
    )
    chatStore.hideMessageForMe(msg.id, chatStore.activeRoomId)
    ElMessage.success('Đã xóa tin nhắn ở phía bạn')
  } catch (e) {
    // Người dùng bấm Hủy
  }
}

// Lấy class animation cho từng emoji micro-interaction
const getEmojiClass = (emoji) => {
  switch (emoji) {
    case '👍': return 'thumbsup'
    case '❤️': return 'heart'
    case '😂': return 'joy'
    case '😮': return 'wow'
    case '😢': return 'cry'
    case '🔥': return 'fire'
    default: return 'general'
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

// Phân giải chuỗi ISO thời gian chuẩn hóa
const parseMessageDate = (isoString) => {
  if (!isoString) return new Date()
  if (isoString instanceof Date) return isoString
  let str = String(isoString).trim()
  if (str.includes('T') && !str.endsWith('Z') && !str.match(/[+-]\d{2}:?\d{2}$/)) {
    return new Date(str + 'Z')
  }
  return new Date(str)
}

// Format thời gian hiển thị
const formatTime = (isoString) => {
  if (!isoString) return ''
  try {
    const d = parseMessageDate(isoString)
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
    const d = parseMessageDate(isoString)
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

// =========================================================================
//                   XỬ LÝ TỆP TIN & TÀI LIỆU ĐÍNH KÈM
// =========================================================================

const parseFileContent = (content) => {
  if (!content) return { url: '', name: 'Tài liệu đính kèm', size: 0, ext: 'FILE' }
  try {
    if (typeof content === 'string' && content.startsWith('{') && content.endsWith('}')) {
      const parsed = JSON.parse(content)
      const name = parsed.name || parsed.fileName || 'Tài liệu đính kèm'
      const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : 'file'
      return {
        url: parsed.url || '',
        name: name,
        size: parsed.size || parsed.fileSize || 0,
        ext: ext
      }
    }
  } catch (e) {}
  const parts = String(content).split('/')
  const rawName = decodeURIComponent(parts[parts.length - 1] || 'Tài liệu đính kèm')
  const ext = rawName.includes('.') ? rawName.split('.').pop().toLowerCase() : 'file'
  return {
    url: content,
    name: rawName,
    size: 0,
    ext: ext
  }
}

const getFileName = (content) => parseFileContent(content).name
const getFileUrl = (content) => parseFileContent(content).url
const getFileSize = (content) => parseFileContent(content).size
const getFileExt = (content) => parseFileContent(content).ext

const formatFileSize = (bytes) => {
  if (!bytes || bytes <= 0) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const isPdf = (content) => getFileExt(content) === 'pdf'
const isZip = (content) => ['zip', 'rar', '7z', 'tar', 'gz'].includes(getFileExt(content))

const getFileThemeClass = (ext) => {
  const e = (ext || '').toLowerCase()
  if (e === 'pdf') return 'theme-pdf'
  if (['doc', 'docx', 'txt', 'rtf'].includes(e)) return 'theme-word'
  if (['xls', 'xlsx', 'csv'].includes(e)) return 'theme-excel'
  if (['ppt', 'pptx'].includes(e)) return 'theme-ppt'
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(e)) return 'theme-zip'
  return 'theme-generic'
}

// Hàm upload và gửi media (ảnh, video, tài liệu)
const uploadAndSendMedia = async (file) => {
  if (!file) return

  const isVideo = file.type.startsWith('video/')
  const isImage = file.type.startsWith('image/')
  const isDoc = !isImage && !isVideo

  // Validate dung lượng: Ảnh < 15MB, Video < 100MB, Tài liệu < 50MB
  if (isImage && file.size > 15 * 1024 * 1024) {
    ElMessage.warning('Kích thước ảnh tối đa cho phép là 15MB!')
    return
  }
  if (isVideo && file.size > 100 * 1024 * 1024) {
    ElMessage.warning('Kích thước video tối đa cho phép là 100MB!')
    return
  }
  if (isDoc && file.size > 50 * 1024 * 1024) {
    ElMessage.warning('Kích thước tệp tài liệu tối đa cho phép là 50MB!')
    return
  }

  isUploadingImage.value = true
  const formData = new FormData()
  formData.append('file', file)

  try {
    let loadingText = 'Đang tải tệp lên Cloud...'
    if (isImage) loadingText = 'Đang tải ảnh lên Cloud...'
    else if (isVideo) loadingText = 'Đang tải video lên Cloud...'
    else loadingText = `Đang tải tệp "${file.name}" lên Cloud...`

    ElMessage.info(loadingText)
    // Không truyền thủ công Content-Type để Axios tự sinh boundary multipart chuẩn
    const res = await api.post('/files/upload', formData)

    const mediaUrl = res.data?.url
    const resourceType = res.data?.resourceType || (isVideo ? 'VIDEO' : (isImage ? 'IMAGE' : 'FILE'))
    const fileName = res.data?.fileName || file.name
    const fileSize = res.data?.fileSize || file.size

    if (!mediaUrl) {
      throw new Error('Không nhận được đường dẫn tệp từ máy chủ')
    }

    if (resourceType === 'FILE') {
      const fileData = { url: mediaUrl, name: fileName, size: fileSize }
      if (chatStore.activeChatType === 'PUBLIC') {
        chatStore.sendPublicFile(fileData, 'GLOBAL')
      } else if (chatStore.activeChatType === 'GROUP' && chatStore.activeGroup) {
        chatStore.sendPublicFile(fileData, `GROUP_${chatStore.activeGroup.id}`)
      } else if (chatStore.activePartner) {
        chatStore.sendPrivateFile(chatStore.activePartner.username, fileData)
      }
      ElMessage.success(`Đã gửi tệp "${fileName}" thành công!`)
    } else if (resourceType === 'VIDEO') {
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

// Xử lý dán (Paste / Ctrl+V) hình ảnh, video hoặc file trực tiếp vào ô chat
const handlePaste = async (event) => {
  const items = (event.clipboardData || window.clipboardData)?.items
  if (!items) return

  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item.kind === 'file') {
      event.preventDefault()
      const file = item.getAsFile()
      if (file) {
        await uploadAndSendMedia(file)
        return
      }
    }
  }
}

// =========================================================================
//                   ĐỊNH DẠNG TIN NHẮN & NHẮC TÊN (MENTION)
// =========================================================================

const escapeHtml = (unsafe) => {
  if (!unsafe) return ''
  return unsafe
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const formatMessageWithMentions = (content) => {
  if (!content) return ''
  let text = escapeHtml(content)
  // Xuống dòng
  text = text.replace(/\n/g, '<br>')

  // Chuyển link HTTP/HTTPS thành clickable
  text = text.replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1" target="_blank" rel="noopener noreferrer" class="chat-inline-link">$1</a>')

  // Định dạng Mention @username
  const myUsername = authStore.user?.username?.toLowerCase()
  text = text.replace(/@([a-zA-Z0-9_\u00C0-\u024F\u1E00-\u1EFF]+)/g, (match, uname) => {
    const isMe = myUsername && uname.toLowerCase() === myUsername
    return `<span class="mention-tag ${isMe ? 'is-me' : ''}">@${uname}</span>`
  })

  // Highlight kết quả tìm kiếm nếu đang mở thanh tìm kiếm
  if (isSearchOpen.value && searchQuery.value.trim()) {
    const q = searchQuery.value.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const regex = new RegExp(`(${q})`, 'gi')
    text = text.replace(regex, '<mark class="search-highlight">$1</mark>')
  }

  return text
}

// Danh sách thành viên có thể tag trong phòng hiện tại
const currentRoomMembersList = computed(() => {
  if (chatStore.activeChatType === 'GROUP' && chatStore.activeGroup) {
    return chatStore.activeGroup.members || []
  }
  if (chatStore.activeChatType === 'PRIVATE' && chatStore.activePartner) {
    const list = [chatStore.activePartner]
    if (authStore.user) list.push(authStore.user)
    return list
  }
  return chatStore.colleagues || []
})

const filteredMentionMembers = computed(() => {
  const q = mentionState.value.query.trim().toLowerCase()
  const members = currentRoomMembersList.value
  if (!q) return members.slice(0, 8)
  return members.filter(m => 
    (m.username || '').toLowerCase().includes(q) ||
    (m.fullName || '').toLowerCase().includes(q)
  ).slice(0, 8)
})

const handleInputWithMention = () => {
  handleTyping()
  checkMentionTrigger()
}

const checkMentionTrigger = () => {
  const el = inputTextareaRef.value
  if (!el) return
  const cursor = el.selectionStart
  const text = inputContent.value.substring(0, cursor)
  const match = text.match(/@([a-zA-Z0-9_\u00C0-\u024F\u1E00-\u1EFF]*)$/)
  if (match) {
    mentionState.value = {
      isOpen: true,
      query: match[1],
      selectedIndex: 0,
      atIndex: match.index
    }
  } else {
    mentionState.value.isOpen = false
  }
}

const handleTextareaKeydown = (e) => {
  if (mentionState.value.isOpen && filteredMentionMembers.value.length > 0) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      mentionState.value.selectedIndex = (mentionState.value.selectedIndex + 1) % filteredMentionMembers.value.length
      return
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      mentionState.value.selectedIndex = (mentionState.value.selectedIndex - 1 + filteredMentionMembers.value.length) % filteredMentionMembers.value.length
      return
    }
    if (e.key === 'Enter' || e.key === 'Tab') {
      e.preventDefault()
      const selected = filteredMentionMembers.value[mentionState.value.selectedIndex]
      if (selected) {
        selectMentionMember(selected)
      }
      return
    }
    if (e.key === 'Escape') {
      e.preventDefault()
      mentionState.value.isOpen = false
      return
    }
  }

  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSendMessage()
  } else if (e.key === 'Escape') {
    cancelEdit()
  }
}

const selectMentionMember = (member) => {
  const el = inputTextareaRef.value
  const cursor = el ? el.selectionStart : inputContent.value.length
  const beforeAt = inputContent.value.substring(0, mentionState.value.atIndex)
  const afterCursor = inputContent.value.substring(cursor)
  inputContent.value = `${beforeAt}@${member.username} ${afterCursor}`
  mentionState.value.isOpen = false
  nextTick(() => {
    if (el) {
      el.focus()
      const newPos = beforeAt.length + member.username.length + 2
      el.setSelectionRange(newPos, newPos)
    }
  })
}

// =========================================================================
//                   TÌM KIẾM TIN NHẮN TRONG PHÒNG (IN-CHAT SEARCH)
// =========================================================================

const toggleSearchBar = () => {
  isSearchOpen.value = !isSearchOpen.value
  if (isSearchOpen.value) {
    currentMatchIndex.value = 0
    nextTick(() => {
      searchInputRef.value?.focus()
    })
  } else {
    searchQuery.value = ''
    highlightedMessageId.value = null
  }
}

const closeSearch = () => {
  isSearchOpen.value = false
  searchQuery.value = ''
  highlightedMessageId.value = null
}

const searchMatchingMessages = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return []
  const msgs = chatStore.currentMessages || []
  return msgs.filter(m => {
    if (m.isDeleted) return false
    if (m.type === 'CHAT') {
      return (m.content || '').toLowerCase().includes(q)
    }
    if (m.type === 'FILE') {
      const p = parseFileContent(m.content)
      return (p.name || '').toLowerCase().includes(q)
    }
    return false
  })
})

const searchMatchCount = computed(() => searchMatchingMessages.value.length)

watch(searchQuery, () => {
  currentMatchIndex.value = 0
  if (searchMatchingMessages.value.length > 0) {
    jumpToSearchMatch(0)
  } else {
    highlightedMessageId.value = null
  }
})

const handleSearchNext = () => {
  if (searchMatchCount.value === 0) return
  currentMatchIndex.value = (currentMatchIndex.value + 1) % searchMatchCount.value
  jumpToSearchMatch(currentMatchIndex.value)
}

const handleSearchPrev = () => {
  if (searchMatchCount.value === 0) return
  currentMatchIndex.value = (currentMatchIndex.value - 1 + searchMatchCount.value) % searchMatchCount.value
  jumpToSearchMatch(currentMatchIndex.value)
}

const jumpToSearchMatch = (idx) => {
  const target = searchMatchingMessages.value[idx]
  if (target) {
    highlightedMessageId.value = target.id
    scrollToMessage(target.id)
  }
}

// =========================================================================
//                   GHIM TIN NHẮN (PINNED MESSAGES)
// =========================================================================

const currentPinnedList = computed(() => {
  return chatStore.currentPinnedMessages || []
})

const currentActivePinnedMessage = computed(() => {
  if (!currentPinnedList.value.length) return null
  const idx = activePinnedIndex.value % currentPinnedList.value.length
  return currentPinnedList.value[idx] || currentPinnedList.value[0]
})

const cyclePinnedMessage = () => {
  if (currentPinnedList.value.length <= 1) return
  activePinnedIndex.value = (activePinnedIndex.value + 1) % currentPinnedList.value.length
}

const getPinnedSnippet = (msg) => {
  if (!msg) return ''
  if (msg.isDeleted) return 'Tin nhắn đã được thu hồi'
  if (msg.type === 'IMAGE') return '📷 [Hình ảnh]'
  if (msg.type === 'VIDEO') return '🎥 [Video]'
  if (msg.type === 'FILE') {
    const p = parseFileContent(msg.content)
    return `📎 [Tệp] ${p.name}`
  }
  return msg.content || ''
}

const scrollToPinnedMessage = () => {
  if (!currentActivePinnedMessage.value) return
  scrollToMessage(currentActivePinnedMessage.value.id)
}

const handleTogglePin = async (msg) => {
  if (!msg || !msg.id) return
  try {
    const isCurrentlyPinned = Boolean(msg.isPinned)
    await chatStore.togglePinMessage(msg.id, !isCurrentlyPinned)
    ElMessage.success(isCurrentlyPinned ? 'Đã bỏ ghim tin nhắn' : 'Đã ghim tin nhắn lên đầu cuộc trò chuyện!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || 'Không thể thao tác ghim tin nhắn!')
  }
}

const handleUnpinMessage = async (messageId) => {
  if (!messageId) return
  try {
    await chatStore.togglePinMessage(messageId, false)
    ElMessage.success('Đã bỏ ghim tin nhắn')
  } catch (err) {
    ElMessage.error('Không thể bỏ ghim tin nhắn')
  }
}

// Bắt đầu Reply tin nhắn
const handleStartReply = (msg) => {
  chatStore.setReplyingTo(msg)
  nextTick(() => {
    inputTextareaRef.value?.focus()
  })
}

// Cuộn đến tin nhắn gốc khi bấm vào quote Reply hoặc kết quả tìm kiếm / ghim
const scrollToMessage = (messageId) => {
  if (!messageId) return
  const el = document.getElementById(`msg-${messageId}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.classList.add('is-highlighted')
    setTimeout(() => {
      el.classList.remove('is-highlighted')
    }, 2500)
  } else {
    ElMessage.info('Tin nhắn này ở xa hơn trong lịch sử cuộn.')
  }
}

// Mở modal xem trước ảnh phóng to
const openImageLightbox = (url) => {
  previewImageUrl.value = url
}

// Phím tắt bàn phím: ESC đóng lightbox/tìm kiếm/mention, Ctrl+F mở tìm kiếm
const handleKeydown = (e) => {
  if ((e.ctrlKey || e.metaKey) && (e.key === 'f' || e.key === 'F')) {
    e.preventDefault()
    toggleSearchBar()
    return
  }
  if (e.key === 'Escape') {
    if (previewImageUrl.value) {
      previewImageUrl.value = null
      return
    }
    if (isSearchOpen.value) {
      closeSearch()
      return
    }
    if (mentionState.value.isOpen) {
      mentionState.value.isOpen = false
      return
    }
  }
}

// Watch chuyển đổi phòng để reset search & ghim
watch(
  () => chatStore.activeRoomId,
  (newRoomId) => {
    activePinnedIndex.value = 0
    closeSearch()
    mentionState.value.isOpen = false
    if (newRoomId) {
      chatStore.fetchPinnedMessages(newRoomId)
    }
  }
)

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


// =========================================================================
//                   TÍNH NĂNG GỌI THOẠI & VIDEO (WEBRTC CALLING)
// =========================================================================
const showEmojiPopover = ref(false)
const isInputFocused = ref(false)

const localVideoRef = ref(null)
const remoteVideoRef = ref(null)
const remoteAudioRef = ref(null)
const isMuted = ref(false)
const isCameraOff = ref(false)
const hasRemoteVideoTrack = ref(false)
const callDurationSeconds = ref(0)
const callDurationFormatted = computed(() => {
  const m = Math.floor(callDurationSeconds.value / 60)
  const s = callDurationSeconds.value % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
})
let callDurationTimer = null
let peerConnection = null
let localMediaStream = null
let remoteMediaStream = null
let pendingCandidates = []

// State & logic cài đặt nhạc chuông cuộc gọi
const showRingtoneModal = ref(false)
const userRingtone = ref(localStorage.getItem('erp_chat_ringtone') || 'modern')
const userRingtoneVolume = ref(Number(localStorage.getItem('erp_chat_ringtone_vol') || 0.8))
const previewingRingtone = ref(null)
let previewTimeout = null

const ringtoneOptions = [
  { id: 'modern', name: 'Du dương hiện đại (Modern Chime)', desc: 'Giai điệu chuông Marimba arpeggio trong trẻo, vui tươi' },
  { id: 'classic', name: 'Điện thoại cổ điển (Classic Bell)', desc: 'Tiếng chuông reo điện thoại văn phòng truyền thống' },
  { id: 'cyber', name: 'Công nghệ Synthwave (Cyber Digital)', desc: 'Hợp âm điện tử đa âm phong cách tương lai' },
  { id: 'gentle', name: 'Nhẹ nhàng thanh lịch (Zen Harmony)', desc: 'Âm sắc sáo mộc mạc du dương, không gây giật mình' }
]

// Ringtones synthesizer (Web Audio API đa âm sắc)
let ringtoneAudioCtx = null
let ringtoneInterval = null

const playToneNote = (ctx, freq, startSec, durationSec, type = 'sine', maxGain = 0.25) => {
  if (!ctx || ctx.state === 'closed') return
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.type = type
  osc.frequency.setValueAtTime(freq, ctx.currentTime + startSec)
  gain.gain.setValueAtTime(0.001, ctx.currentTime + startSec)
  gain.gain.linearRampToValueAtTime(maxGain * userRingtoneVolume.value, ctx.currentTime + startSec + 0.03)
  gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + startSec + durationSec)
  osc.connect(gain)
  gain.connect(ctx.destination)
  osc.start(ctx.currentTime + startSec)
  osc.stop(ctx.currentTime + startSec + durationSec)
}

const triggerRingtoneMelody = (ctx, typeId) => {
  if (!ctx || ctx.state === 'closed') return
  const vol = Math.max(0.05, Math.min(1, userRingtoneVolume.value))
  if (typeId === 'classic') {
    // Dual frequency telephone ring: 440Hz + 480Hz modulated
    for (let i = 0; i < 4; i++) {
      playToneNote(ctx, 440, i * 0.12, 0.08, 'sine', 0.2 * vol)
      playToneNote(ctx, 480, i * 0.12, 0.08, 'triangle', 0.15 * vol)
    }
  } else if (typeId === 'cyber') {
    // Synthwave pulse
    playToneNote(ctx, 523.25, 0, 0.15, 'sawtooth', 0.12 * vol) // C5
    playToneNote(ctx, 659.25, 0.12, 0.15, 'sawtooth', 0.12 * vol) // E5
    playToneNote(ctx, 783.99, 0.24, 0.25, 'triangle', 0.15 * vol) // G5
    playToneNote(ctx, 1046.50, 0.38, 0.4, 'sine', 0.2 * vol) // C6
  } else if (typeId === 'gentle') {
    // Zen harmony chime
    playToneNote(ctx, 587.33, 0, 0.4, 'sine', 0.18 * vol) // D5
    playToneNote(ctx, 739.99, 0.18, 0.5, 'sine', 0.18 * vol) // F#5
    playToneNote(ctx, 880.00, 0.36, 0.6, 'sine', 0.2 * vol) // A5
  } else {
    // modern (default): Marimba arpeggio
    playToneNote(ctx, 523.25, 0, 0.18, 'triangle', 0.22 * vol) // C5
    playToneNote(ctx, 659.25, 0.12, 0.18, 'triangle', 0.22 * vol) // E5
    playToneNote(ctx, 783.99, 0.24, 0.22, 'triangle', 0.24 * vol) // G5
    playToneNote(ctx, 1046.50, 0.38, 0.45, 'sine', 0.25 * vol) // C6
  }
}

const playRingtone = (overrideType = null) => {
  stopRingtone()
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) return
    ringtoneAudioCtx = new AudioContextClass()
    if (ringtoneAudioCtx.state === 'suspended') {
      ringtoneAudioCtx.resume().catch(e => console.warn('AudioContext resume:', e))
    }
    const chosen = overrideType || userRingtone.value || 'modern'
    triggerRingtoneMelody(ringtoneAudioCtx, chosen)
    ringtoneInterval = setInterval(() => {
      triggerRingtoneMelody(ringtoneAudioCtx, chosen)
    }, 2200)
  } catch (e) {
    console.warn('Không thể phát ringtone:', e)
  }
}

const stopRingtone = () => {
  if (ringtoneInterval) {
    clearInterval(ringtoneInterval)
    ringtoneInterval = null
  }
  if (ringtoneAudioCtx) {
    try { ringtoneAudioCtx.close() } catch (e) {}
    ringtoneAudioCtx = null
  }
}

const previewRingtone = (toneId) => {
  if (previewingRingtone.value === toneId) {
    stopRingtonePreview()
    return
  }
  stopRingtonePreview()
  previewingRingtone.value = toneId
  playRingtone(toneId)
  previewTimeout = setTimeout(() => {
    stopRingtonePreview()
  }, 4500)
}

const stopRingtonePreview = () => {
  previewingRingtone.value = null
  if (previewTimeout) {
    clearTimeout(previewTimeout)
    previewTimeout = null
  }
  stopRingtone()
}

const saveRingtone = (id) => {
  userRingtone.value = id
  localStorage.setItem('erp_chat_ringtone', id)
  localStorage.setItem('erp_chat_ringtone_vol', userRingtoneVolume.value)
  stopRingtonePreview()
  showRingtoneModal.value = false
  ElMessage.success('Đã lưu cấu hình nhạc chuông cuộc gọi!')
}

const startCallDurationTimer = () => {
  stopCallDurationTimer()
  callDurationSeconds.value = 0
  callDurationTimer = setInterval(() => {
    callDurationSeconds.value++
  }, 1000)
}

const stopCallDurationTimer = () => {
  if (callDurationTimer) {
    clearInterval(callDurationTimer)
    callDurationTimer = null
  }
}

// Gắn luồng video/audio vào các phần tử DOM tương ứng
const attachMediaStreams = () => {
  nextTick(() => {
    if (localVideoRef.value && localMediaStream) {
      if (localVideoRef.value.srcObject !== localMediaStream) {
        localVideoRef.value.srcObject = localMediaStream
      }
      localVideoRef.value.play().catch(e => console.warn('Lỗi play local video:', e))
    }
    if (remoteVideoRef.value && remoteMediaStream) {
      if (remoteVideoRef.value.srcObject !== remoteMediaStream) {
        remoteVideoRef.value.srcObject = remoteMediaStream
      }
      remoteVideoRef.value.play().catch(e => console.warn('Lỗi play remote video:', e))
    }
    if (remoteAudioRef.value && remoteMediaStream) {
      if (remoteAudioRef.value.srcObject !== remoteMediaStream) {
        remoteAudioRef.value.srcObject = remoteMediaStream
      }
      remoteAudioRef.value.play().catch(e => console.warn('Lỗi play remote audio:', e))
    }
  })
}

// Đệm và xử lý ICE Candidate chống mất gói khi đang đổ chuông
const addOrQueueCandidate = async (candidate) => {
  if (!candidate) return
  if (peerConnection && peerConnection.remoteDescription && peerConnection.remoteDescription.type) {
    try {
      await peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
    } catch (e) {
      console.warn('Lỗi addIceCandidate:', e)
    }
  } else {
    pendingCandidates.push(candidate)
  }
}

const flushPendingCandidates = async () => {
  if (!peerConnection || !peerConnection.remoteDescription) return
  while (pendingCandidates.length > 0) {
    const cand = pendingCandidates.shift()
    try {
      await peerConnection.addIceCandidate(new RTCIceCandidate(cand))
    } catch (e) {
      console.warn('Lỗi flush candidate:', e)
    }
  }
}

// Tạo luồng âm thanh mô phỏng để WebRTC vẫn kết nối 100% khi test trên máy không có microphone
const createSilentAudioStream = () => {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext
    if (!AudioCtx) return new MediaStream()
    const ctx = new AudioCtx()
    const osc = ctx.createOscillator()
    const dst = ctx.createMediaStreamDestination()
    osc.connect(dst)
    osc.start()
    const track = dst.stream.getAudioTracks()[0]
    if (track) track.enabled = false
    return dst.stream
  } catch (e) {
    console.warn('Không thể khởi tạo silent stream:', e)
    return new MediaStream()
  }
}

// Lấy media stream với cơ chế tự động fallback nếu không có camera hoặc microphone
const getMediaStream = async (type) => {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    console.warn('Trình duyệt không hỗ trợ getUserMedia, sử dụng luồng âm thanh mô phỏng')
    return createSilentAudioStream()
  }

  try {
    if (type === 'audio') {
      return await navigator.mediaDevices.getUserMedia({ audio: true, video: false })
    }
    // video
    try {
      return await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: { width: { ideal: 1280 }, height: { ideal: 720 } }
      })
    } catch (camErr) {
      console.warn('Không tìm thấy camera hoặc bị từ chối, chuyển sang gọi thoại:', camErr)
      ElMessage.warning('Không tìm thấy camera trên máy, tự động chuyển sang gọi thoại!')
      chatStore.callState.callType = 'audio'
      return await navigator.mediaDevices.getUserMedia({ audio: true, video: false })
    }
  } catch (audioErr) {
    console.warn('Không thể truy cập microphone phần cứng:', audioErr)
    ElMessage.warning('Không phát hiện Microphone phần cứng hoặc quyền bị hạn chế. Hệ thống tự động kết nối mô phỏng để test!')
    return createSilentAudioStream()
  }
}

const createPeerConnection = (targetUsername) => {
  const pc = new RTCPeerConnection({
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
      { urls: 'stun:stun2.l.google.com:19302' },
      { urls: 'stun:stun.services.mozilla.com' }
    ]
  })

  pc.onicecandidate = (event) => {
    if (event.candidate) {
      chatStore.sendCallSignal({
        type: 'call-candidate',
        targetUsername,
        candidate: event.candidate
      })
    }
  }

  pc.ontrack = (event) => {
    console.log('WebRTC ontrack received:', event.track.kind, event.streams)
    const stream = (event.streams && event.streams[0]) ? event.streams[0] : new MediaStream([event.track])
    remoteMediaStream = stream
    if (event.track.kind === 'video') {
      hasRemoteVideoTrack.value = true
    }
    attachMediaStreams()
  }

  pc.onconnectionstatechange = () => {
    console.log('WebRTC connection state:', pc.connectionState)
    if (pc.connectionState === 'connected') {
      stopRingtone()
      chatStore.callState.status = 'CONNECTED'
      startCallDurationTimer()
      attachMediaStreams()
    } else if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed' || pc.connectionState === 'closed') {
      cleanupCall()
    }
  }

  return pc
}

const startVoiceCall = async () => {
  if (!chatStore.activePartner?.username) return
  await startCall('audio')
}

const startVideoCall = async () => {
  if (!chatStore.activePartner?.username) return
  await startCall('video')
}

const startCall = async (type = 'video') => {
  const partner = chatStore.activePartner
  if (!partner) return

  if (!chatStore.isUserOnline(partner.username)) {
    ElMessage.warning(`Đồng nghiệp "${partner.fullName || partner.username}" hiện đang ngoại tuyến, không thể nhận cuộc gọi!`)
    return
  }

  try {
    localMediaStream = await getMediaStream(type)
  } catch (err) {
    console.error('Không thể truy cập Microphone/Camera:', err)
    ElMessage.error('Vui lòng cho phép truy cập Microphone & Camera để thực hiện cuộc gọi!')
    return
  }

  isMuted.value = false
  isCameraOff.value = false
  hasRemoteVideoTrack.value = false
  pendingCandidates = []

  chatStore.callState = {
    isOpen: true,
    isIncoming: false,
    callType: type,
    partnerUsername: partner.username,
    partnerFullName: partner.fullName || partner.username,
    partnerAvatarUrl: partner.avatarUrl,
    status: 'CALLING',
    offerData: null
  }

  attachMediaStreams()
  playRingtone()

  peerConnection = createPeerConnection(partner.username)
  localMediaStream.getTracks().forEach(track => {
    peerConnection.addTrack(track, localMediaStream)
  })

  try {
    const offer = await peerConnection.createOffer()
    await peerConnection.setLocalDescription(offer)

    chatStore.sendCallSignal({
      type: 'call-request',
      callType: type,
      targetUsername: partner.username,
      offer
    })
  } catch (e) {
    console.error('Lỗi khởi tạo WebRTC offer:', e)
    cleanupCall()
    ElMessage.error('Không thể khởi tạo đường truyền cuộc gọi!')
  }
}

const handleAcceptIncomingCall = async () => {
  stopRingtone()
  const callerUsername = chatStore.callState.partnerUsername
  const callType = chatStore.callState.callType || 'video'
  const offerData = chatStore.callState.offerData

  // 1. Chuyển trạng thái giao diện sang Active Call ngay lập tức
  chatStore.callState.isIncoming = false
  chatStore.callState.status = 'CONNECTED'
  startCallDurationTimer()

  // 2. Lấy stream Microphone / Camera
  try {
    localMediaStream = await getMediaStream(callType)
  } catch (err) {
    console.error('Lỗi getUserMedia khi nhận cuộc gọi:', err)
    ElMessage.error('Không thể truy cập Micro/Camera!')
    handleDeclineIncomingCall()
    return
  }

  isMuted.value = false
  isCameraOff.value = false
  hasRemoteVideoTrack.value = false
  attachMediaStreams()

  // 3. Khởi tạo PeerConnection
  peerConnection = createPeerConnection(callerUsername)
  localMediaStream.getTracks().forEach(track => {
    peerConnection.addTrack(track, localMediaStream)
  })

  // 4. Set remote description offer
  try {
    await peerConnection.setRemoteDescription(new RTCSessionDescription(offerData))
    await flushPendingCandidates()

    const answer = await peerConnection.createAnswer()
    await peerConnection.setLocalDescription(answer)

    chatStore.sendCallSignal({
      type: 'call-accept',
      targetUsername: callerUsername,
      answer
    })
  } catch (e) {
    console.error('Lỗi khi thiết lập answer WebRTC:', e)
    cleanupCall()
    ElMessage.error('Không thể kết nối cuộc gọi!')
  }
}

const handleDeclineIncomingCall = () => {
  stopRingtone()
  const callerUsername = chatStore.callState.partnerUsername
  chatStore.sendCallSignal({
    type: 'call-reject',
    targetUsername: callerUsername
  })
  cleanupCall()
}

const hangUpCall = () => {
  const partnerUsername = chatStore.callState.partnerUsername
  if (partnerUsername) {
    chatStore.sendCallSignal({
      type: 'call-end',
      targetUsername: partnerUsername
    })
  }
  cleanupCall()
}

const cleanupCall = () => {
  stopRingtone()
  stopCallDurationTimer()

  if (localMediaStream) {
    localMediaStream.getTracks().forEach(track => track.stop())
    localMediaStream = null
  }
  if (remoteMediaStream) {
    remoteMediaStream.getTracks().forEach(track => track.stop())
    remoteMediaStream = null
  }
  if (peerConnection) {
    peerConnection.close()
    peerConnection = null
  }
  pendingCandidates = []
  hasRemoteVideoTrack.value = false

  if (localVideoRef.value) localVideoRef.value.srcObject = null
  if (remoteVideoRef.value) remoteVideoRef.value.srcObject = null
  if (remoteAudioRef.value) remoteAudioRef.value.srcObject = null

  chatStore.resetCallState()
}

const toggleMuteAudio = () => {
  if (!localMediaStream) return
  isMuted.value = !isMuted.value
  localMediaStream.getAudioTracks().forEach(track => {
    track.enabled = !isMuted.value
  })
}

const toggleCameraVideo = () => {
  if (!localMediaStream) return
  isCameraOff.value = !isCameraOff.value
  localMediaStream.getVideoTracks().forEach(track => {
    track.enabled = !isCameraOff.value
  })
}

watch(
  () => [chatStore.callState.isOpen, chatStore.callState.isIncoming],
  ([isOpen, isIncoming]) => {
    if (isOpen && !isIncoming) {
      attachMediaStreams()
    }
  }
)

let unregisterCallListener = null

onMounted(() => {
  unregisterCallListener = chatStore.registerCallSignalListener(async (signal) => {
    if (signal.type === 'call-request') {
      playRingtone()
    } else if (signal.type === 'call-accept') {
      stopRingtone()
      chatStore.callState.status = 'CONNECTED'
      startCallDurationTimer()
      if (peerConnection && signal.answer) {
        try {
          await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.answer))
          await flushPendingCandidates()
        } catch (e) {
          console.error('Lỗi setRemoteDescription answer:', e)
        }
      }
      attachMediaStreams()
    } else if (signal.type === 'call-candidate') {
      await addOrQueueCandidate(signal.candidate)
    } else if (signal.type === 'call-reject') {
      stopRingtone()
      ElMessage.warning('Đối phương đã từ chối cuộc gọi!')
      cleanupCall()
    } else if (signal.type === 'call-end') {
      stopRingtone()
      ElMessage.info('Cuộc gọi đã kết thúc.')
      cleanupCall()
    } else if (signal.type === 'call-busy') {
      stopRingtone()
      ElMessage.warning('Đối phương đang trong cuộc gọi khác!')
      cleanupCall()
    }
  })

  // Đảm bảo WebSocket STOMP đã kích hoạt
  chatStore.connect()
  // Tải danh bạ đồng nghiệp
  chatStore.fetchColleagues()
  // Tải danh sách nhóm chat của tôi
  chatStore.fetchMyGroups()

  window.addEventListener('resize', handleResize)
  // Nếu là desktop (> 768px), mở mặc định phòng chung; nếu là mobile (<= 768px), mở danh bạ trước
  if (typeof window !== 'undefined' && window.innerWidth > 768) {
    selectPublicRoom()
  } else {
    showMobileChat.value = false
  }

  // Lắng nghe sự kiện ESC để đóng lightbox/menu
  window.addEventListener('keydown', handleKeydown)
  // Lắng nghe sự kiện quay lại tab hoặc focus cửa sổ để cập nhật đã đọc
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('visibilitychange', handleWindowFocus)
  // Đóng các menu nổi (thao tác tin nhắn, menu header) khi click ra ngoài
  document.addEventListener('click', closeAllFloatingMenus)
})

onUnmounted(() => {
  if (unregisterCallListener) unregisterCallListener()
  cleanupCall()

  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('visibilitychange', handleWindowFocus)
  document.removeEventListener('click', closeAllFloatingMenus)
  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
  }
  if (voiceProgressTimer) {
    clearInterval(voiceProgressTimer)
  }
  if (recordingTimer) {
    clearInterval(recordingTimer)
  }
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
  background: #eff6ff !important;
  border: 1px solid #bfdbfe !important;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08) !important;
  border-radius: 10px;
}

/* Nút ghim cuộc trò chuyện (Pin room) */
.btn-pin-room {
  background: transparent;
  border: none;
  padding: 4px;
  border-radius: 4px;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.15s ease;
  margin-left: 4px;
  flex-shrink: 0;
}

.directory-item:hover .btn-pin-room,
.btn-pin-room.is-pinned {
  opacity: 1;
}

.btn-pin-room.is-pinned {
  color: #f59e0b;
}

.btn-pin-room:hover {
  background: #f1f5f9;
  color: #d97706;
}

/* Nút tạo nhóm dạng pill gọn gàng trên tiêu đề */
.btn-create-group-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-create-group-pill:hover {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
}

.directory-item.is-active .item-name {
  color: #1d4ed8 !important;
  font-weight: 700 !important;
}

.directory-item.is-active .item-sub {
  color: #2563eb !important;
}

.directory-item.is-active .item-avatar {
  box-shadow: 0 0 0 2px #2563eb !important;
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
  min-width: 0;
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
  min-width: 60px;
  max-width: 100%;
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
  background: var(--chat-theme-gradient, linear-gradient(135deg, #2563eb, #1d4ed8));
  color: #ffffff;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
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
  padding: 6px 16px 10px 16px;
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
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  font-size: 13.5px;
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
  gap: 5px;
  padding: 0 14px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  height: 32px;
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
  gap: 4px;
  padding: 3px 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.04);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease, transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform: scale(0.92);
  z-index: 5;
}

.message-bubble-row:hover .message-actions-hover {
  opacity: 1;
  pointer-events: auto;
  transform: scale(1);
}

.hover-reactions-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.btn-hover-reaction {
  background: transparent;
  border: none;
  font-size: 16px;
  padding: 3px 5px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.animated-emoji-char {
  display: inline-block;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  will-change: transform;
}

.btn-hover-reaction:hover {
  background: #f1f5f9;
  transform: scale(1.35) translateY(-3px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
  z-index: 2;
}

/* Micro-interaction animations for emojis */
.emoji-heart:hover .animated-emoji-char {
  animation: heartPulseAnim 0.7s infinite alternate ease-in-out;
}
@keyframes heartPulseAnim {
  0% { transform: scale(1); }
  50% { transform: scale(1.4); }
  100% { transform: scale(1.15); }
}

.emoji-fire:hover .animated-emoji-char {
  animation: fireFlickerAnim 0.6s infinite alternate ease-in-out;
}
@keyframes fireFlickerAnim {
  0% { transform: scale(1) rotate(-4deg); }
  50% { transform: scale(1.35) rotate(6deg) translateY(-2px); }
  100% { transform: scale(1.15) rotate(-3deg); }
}

.emoji-joy:hover .animated-emoji-char {
  animation: joyWiggleAnim 0.6s infinite alternate ease-in-out;
}
@keyframes joyWiggleAnim {
  0% { transform: rotate(-10deg) scale(1.1); }
  100% { transform: rotate(10deg) scale(1.35); }
}

.emoji-thumbsup:hover .animated-emoji-char {
  animation: thumbBounceAnim 0.5s ease;
}
@keyframes thumbBounceAnim {
  0% { transform: translateY(0) scale(1); }
  40% { transform: translateY(-5px) scale(1.35) rotate(-8deg); }
  100% { transform: translateY(-2px) scale(1.25); }
}

.emoji-wow:hover .animated-emoji-char {
  animation: wowPopAnim 0.6s infinite alternate ease-in-out;
}
@keyframes wowPopAnim {
  0% { transform: scale(1); }
  100% { transform: scale(1.4); }
}

.emoji-cry:hover .animated-emoji-char {
  animation: cryShakeAnim 0.6s infinite alternate ease-in-out;
}
@keyframes cryShakeAnim {
  0% { transform: translateY(0); }
  100% { transform: translateY(2px) scale(1.25); }
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

.section-actions-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-mark-all-read {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-mark-all-read:hover {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
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

/* =========================================================================
   IN-CHAT SEARCH BAR (TÌM KIẾM TIN NHẮN TRONG PHÒNG)
   ========================================================================= */
.btn-icon-action.is-search-active {
  background: #2563eb;
  color: #ffffff;
}

.in-chat-search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 18px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  z-index: 10;
  animation: searchSlideDown 0.2s ease-out;
}

@keyframes searchSlideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.search-input-box {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 6px 12px;
  transition: all 0.15s ease;
}

.search-input-box:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.search-icon-svg {
  color: #94a3b8;
  flex-shrink: 0;
}

.search-text-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 13px;
  color: #1e293b;
  background: transparent;
}

.search-counter-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #64748b;
  flex-shrink: 0;
}

.search-counter-badge.has-matches {
  background: #eff6ff;
  color: #2563eb;
}

.search-nav-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-search-step, .btn-search-exit {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-search-step:hover:not(:disabled), .btn-search-exit:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.btn-search-step:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-search-exit:hover {
  background: #fee2e2;
  color: #dc2626;
  border-color: #fca5a5;
}

/* Highlight từ khóa tìm kiếm trong bubble */
.search-highlight {
  background: #fef08a !important;
  color: #854d0e !important;
  padding: 1px 3px;
  border-radius: 3px;
  font-weight: 700;
}

/* =========================================================================
   PINNED MESSAGE BANNER (THANH TIN NHẮN ĐÃ GHIM)
   ========================================================================= */
.pinned-message-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 18px;
  background: #eff6ff;
  border-bottom: 1px solid #dbeafe;
  z-index: 9;
  animation: pinnedFadeIn 0.2s ease-out;
}

@keyframes pinnedFadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.pinned-banner-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 6px;
  transition: background 0.15s;
}

.pinned-banner-main:hover {
  background: rgba(219, 234, 254, 0.45);
}

.pinned-icon-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #3b82f6;
  color: #ffffff;
  flex-shrink: 0;
}

.pinned-text-column {
  flex: 1;
  min-width: 0;
}

.pinned-author-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  line-height: 1.2;
}

.pinned-badge-title {
  font-weight: 700;
  color: #1d4ed8;
}

.pinned-counter-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 8px;
  background: #bfdbfe;
  color: #1e40af;
}

.pinned-author-name {
  color: #475569;
  font-size: 11px;
  font-weight: 500;
}

.pinned-preview-snippet {
  font-size: 12px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.pinned-banner-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.btn-pinned-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 5px;
  border: 1px solid #bfdbfe;
  background: #ffffff;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-pinned-action:hover {
  background: #dbeafe;
}

.btn-unpin-trigger:hover {
  background: #fee2e2;
  color: #dc2626;
  border-color: #fca5a5;
}

/* Nút ghim trên hover actions */
.btn-hover-pin {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 5px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-hover-pin:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.btn-hover-pin.is-pinned-active {
  color: #2563eb;
  background: #eff6ff;
}

.meta-pinned-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  font-weight: 600;
  color: #eab308;
  margin-right: 4px;
}

/* =========================================================================
   FILE & DOCUMENT CARD (THẺ HIỂN THỊ TỆP TIN / TÀI LIỆU)
   ========================================================================= */
.bubble-file-wrapper {
  max-width: 360px;
  width: 100%;
}

.file-card-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: background 0.15s;
}

.is-my-message .file-card-box {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
}

.file-icon-square {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  flex-shrink: 0;
}

.file-icon-square.theme-pdf {
  background: #fee2e2;
  color: #dc2626;
}

.file-icon-square.theme-word {
  background: #dbeafe;
  color: #2563eb;
}

.file-icon-square.theme-excel {
  background: #dcfce7;
  color: #16a34a;
}

.file-icon-square.theme-ppt {
  background: #ffedd5;
  color: #ea580c;
}

.file-icon-square.theme-zip {
  background: #f3e8ff;
  color: #9333ea;
}

.file-icon-square.theme-generic {
  background: #f1f5f9;
  color: #475569;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-card-title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: inherit;
}

.file-card-badges {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
}

.file-ext-chip {
  font-size: 9px;
  font-weight: 700;
  padding: 1px 4px;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.08);
  letter-spacing: 0.5px;
}

.is-my-message .file-ext-chip {
  background: rgba(255, 255, 255, 0.25);
}

.file-size-chip {
  font-size: 11px;
  opacity: 0.8;
}

.btn-file-dl {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  transition: transform 0.15s, background 0.15s;
  text-decoration: none;
}

.btn-file-dl:hover {
  transform: scale(1.08);
  background: #eff6ff;
}

.is-my-message .btn-file-dl {
  background: #ffffff;
  color: #1d4ed8;
}

/* =========================================================================
   MENTION AUTOCOMPLETE POPUP (GỢI Ý NHẮC TÊN @username)
   ========================================================================= */
.mention-autocomplete-card {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 18px;
  width: 320px;
  max-width: calc(100% - 36px);
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 50;
  animation: mentionPopupIn 0.15s ease-out;
}

@keyframes mentionPopupIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.mention-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}

.mention-card-title {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.mention-kbd-guide {
  font-size: 10px;
  color: #94a3b8;
}

.mention-items-list {
  list-style: none;
  margin: 0;
  padding: 4px;
  max-height: 220px;
  overflow-y: auto;
}

.mention-choice-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.12s ease;
}

.mention-choice-row:hover,
.mention-choice-row.is-focused {
  background: #eff6ff;
}

.mention-avatar-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  overflow: hidden;
  flex-shrink: 0;
}

.mention-avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mention-user-details {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.mention-name-text {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mention-handle-text {
  font-size: 11px;
  color: #3b82f6;
}

/* =========================================================================
   MENTION TAG TRONG TIN NHẮN (TAG BADGE)
   ========================================================================= */
.mention-tag {
  display: inline-block;
  padding: 0 5px;
  margin: 0 1px;
  border-radius: 4px;
  font-weight: 600;
  background: rgba(59, 130, 246, 0.14);
  color: #1d4ed8;
}

.is-my-message .mention-tag {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

.mention-tag.is-me {
  background: #fef08a !important;
  color: #854d0e !important;
  font-weight: 700 !important;
  box-shadow: 0 0 0 1px #facc15;
}

/* Đường link trong tin nhắn */
.chat-inline-link {
  color: #2563eb;
  text-decoration: underline;
  word-break: break-all;
}

.is-my-message .chat-inline-link {
  color: #ffffff;
  text-decoration: underline;
}

/* Hiệu ứng chớp sáng khi cuộn đến tin nhắn ghim / tìm kiếm */
.message-item.is-highlighted .message-bubble,
.message-bubble.is-search-matched {
  animation: glowPulse 2.5s ease-out !important;
}

@keyframes glowPulse {
  0% {
    box-shadow: 0 0 0 4px #3b82f6, 0 0 15px rgba(59, 130, 246, 0.5);
    transform: scale(1.02);
  }
  50% {
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.4), 0 0 10px rgba(59, 130, 246, 0.3);
  }
  100% {
    box-shadow: none;
    transform: scale(1);
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* =========================================================================
   COMPACT MESSAGE 3-DOTS MENU & DROPDOWN
   ========================================================================= */
.msg-more-menu-container {
  position: relative;
  display: inline-flex;
}

.btn-hover-more {
  background: transparent;
  border: none;
  color: #64748b;
  padding: 3px 5px;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.btn-hover-more:hover,
.btn-hover-more.is-active {
  background: #f1f5f9;
  color: #0f172a;
}

.msg-dropdown-menu {
  position: absolute;
  right: 0;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.12), 0 8px 10px -6px rgba(0, 0, 0, 0.08);
  min-width: 165px;
  padding: 6px;
  z-index: 100;
  animation: popoverFadeIn 0.15s ease-out;
}

.msg-dropdown-menu.is-up {
  bottom: calc(100% + 6px);
  top: auto;
}

.msg-dropdown-menu.is-down {
  top: calc(100% + 6px);
  bottom: auto;
}

.is-incoming .msg-dropdown-menu {
  right: auto;
  left: 0;
}

.menu-page-pane {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dropdown-item.is-more-step {
  color: #2563eb;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dropdown-item.is-more-step .chevron-step {
  margin-left: auto;
}

.dropdown-item.is-back-step {
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  border-bottom: 1px dashed #e2e8f0;
  padding-bottom: 6px;
  margin-bottom: 4px;
}

.dropdown-item.is-back-step:hover {
  color: #0f172a;
}

.dropdown-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.12s ease;
}

.dropdown-item:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.dropdown-item.is-danger {
  color: #ef4444;
}

.dropdown-item.is-danger:hover {
  background: #fef2f2;
  color: #dc2626;
}

.dropdown-menu-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 4px 0;
}

@keyframes popoverFadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* =========================================================================
   CONSOLIDATED HEADER MORE MENU (TÙY CHỌN HEADER GỌN GÀNG)
   ========================================================================= */
.header-more-menu-wrapper {
  position: relative;
  display: inline-flex;
}

.btn-header-more.is-active {
  background: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
}

.header-dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 235px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.12), 0 8px 10px -6px rgba(0, 0, 0, 0.08);
  padding: 6px;
  z-index: 120;
  animation: popoverFadeIn 0.15s ease-out;
}

.header-menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
}

.header-menu-item:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.header-menu-item svg {
  flex-shrink: 0;
  color: #64748b;
  transition: color 0.15s ease;
}

.header-menu-item:hover svg {
  color: #2563eb;
}

.popover-fade-enter-active,
.popover-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.popover-fade-enter-from,
.popover-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* =========================================================================
   VOICE MESSAGE BUBBLE & AUDIO PLAYER
   ========================================================================= */
.bubble-voice-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px;
  min-width: 210px;
}

.btn-voice-play {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.07);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: inherit;
  transition: transform 0.1s ease, background 0.15s ease;
  flex-shrink: 0;
}

.is-outgoing .btn-voice-play {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

.btn-voice-play:hover {
  transform: scale(1.06);
}

.voice-waveform-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.voice-bars {
  display: flex;
  align-items: center;
  gap: 3px;
  height: 20px;
}

.waveform-bar {
  width: 3px;
  height: 8px;
  background: currentColor;
  opacity: 0.5;
  border-radius: 2px;
  transition: height 0.2s ease, opacity 0.2s ease;
}

.waveform-bar:nth-child(2n) { height: 14px; }
.waveform-bar:nth-child(3n) { height: 18px; }
.waveform-bar:nth-child(4n) { height: 10px; }

.waveform-bar.is-animating {
  opacity: 1;
  animation: wavePulse 0.8s ease-in-out infinite alternate;
}

.waveform-bar.is-animating:nth-child(2n) { animation-delay: 0.15s; }
.waveform-bar.is-animating:nth-child(3n) { animation-delay: 0.3s; }
.waveform-bar.is-animating:nth-child(4n) { animation-delay: 0.45s; }

@keyframes wavePulse {
  0% { height: 4px; }
  100% { height: 20px; }
}

.voice-duration {
  font-size: 11px;
  opacity: 0.8;
  font-variant-numeric: tabular-nums;
}

/* =========================================================================
   POLL MESSAGE BUBBLE
   ========================================================================= */
.bubble-poll-wrapper {
  min-width: 250px;
  max-width: 380px;
  padding: 4px 2px;
}

.poll-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.1);
}

.is-outgoing .poll-header {
  border-bottom-color: rgba(255, 255, 255, 0.2);
}

.poll-icon-box {
  font-size: 22px;
}

.poll-title-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.poll-question {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: inherit;
  line-height: 1.4;
}

.poll-type-badge {
  font-size: 11px;
  opacity: 0.75;
}

.poll-options-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 8px;
}

.poll-option-row {
  position: relative;
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.15s ease;
}

.is-outgoing .poll-option-row {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
}

.poll-option-row:hover {
  background: rgba(0, 0, 0, 0.07);
}

.is-outgoing .poll-option-row:hover {
  background: rgba(255, 255, 255, 0.2);
}

.poll-option-row.has-voted {
  border-color: #3b82f6;
  font-weight: 600;
}

.is-outgoing .poll-option-row.has-voted {
  border-color: #ffffff;
}

.poll-option-progress-bg {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  background: rgba(59, 130, 246, 0.18);
  border-radius: 7px;
  transition: width 0.3s ease;
  z-index: 1;
}

.is-outgoing .poll-option-progress-bg {
  background: rgba(255, 255, 255, 0.28);
}

.poll-option-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 2;
  font-size: 13px;
}

.poll-check-icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.poll-empty-circle {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid currentColor;
  opacity: 0.5;
}

.poll-option-text {
  flex: 1;
  word-break: break-word;
}

.poll-vote-count {
  font-size: 11px;
  opacity: 0.8;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.poll-footer {
  font-size: 11px;
  opacity: 0.75;
  text-align: right;
}

/* =========================================================================
   INPUT TOOLS & LIVE VOICE RECORDING BAR
   ========================================================================= */
.input-tools-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-attach-voice,
.btn-attach-poll {
  background: transparent;
  border: none;
  color: #64748b;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-attach-voice:hover,
.btn-attach-poll:hover {
  background: #f1f5f9;
  color: #2563eb;
}

.btn-attach-voice.is-recording-active {
  background: #fee2e2;
  color: #dc2626;
  animation: recordPulse 1s infinite alternate;
}

@keyframes recordPulse {
  0% { transform: scale(1); }
  100% { transform: scale(1.1); }
}

.voice-recording-inline-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  padding: 8px 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
}

.recording-pulse-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ef4444;
  animation: pulseDot 1s infinite;
  flex-shrink: 0;
}

@keyframes pulseDot {
  0% { transform: scale(0.8); opacity: 0.6; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.8); opacity: 0.6; }
}

.recording-timer-text {
  font-size: 13px;
  font-weight: 700;
  color: #b91c1c;
  font-variant-numeric: tabular-nums;
}

.recording-waves-animation {
  display: flex;
  align-items: center;
  gap: 2px;
  height: 20px;
  flex: 1;
}

.wave-stick {
  width: 3px;
  height: 6px;
  background: #ef4444;
  border-radius: 2px;
  animation: waveStick 0.6s infinite alternate ease-in-out;
}

.wave-stick:nth-child(2n) { animation-delay: 0.1s; height: 12px; }
.wave-stick:nth-child(3n) { animation-delay: 0.2s; height: 18px; }
.wave-stick:nth-child(4n) { animation-delay: 0.3s; height: 9px; }

@keyframes waveStick {
  0% { height: 4px; }
  100% { height: 22px; }
}

.recording-status-msg {
  font-size: 12px;
  color: #991b1b;
  white-space: nowrap;
}

.recording-btn-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-cancel-recording {
  background: #ffffff;
  border: 1px solid #fca5a5;
  color: #dc2626;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-cancel-recording:hover {
  background: #fee2e2;
}

.btn-send-recording {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border: none;
  color: #ffffff;
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.35);
  transition: all 0.15s ease;
}

.btn-send-recording:hover {
  background: #b91c1c;
}

/* =========================================================================
   CỘT PHẢI SLIDING DRAWER (MEDIA, TỆP TIN & LIÊN KẾT)
   ========================================================================= */
.chat-right-drawer {
  width: 340px;
  background: #ffffff;
  border-left: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  z-index: 15;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.drawer-title-group {
  display: flex;
  flex-direction: column;
}

.drawer-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.drawer-subtitle {
  font-size: 11px;
  color: #64748b;
}

.btn-close-drawer {
  background: transparent;
  border: none;
  font-size: 16px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.btn-close-drawer:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.drawer-nav-tabs {
  display: flex;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 4px;
}

.drawer-tab {
  flex: 1;
  padding: 8px 4px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.drawer-tab:hover {
  color: #1e293b;
}

.drawer-tab.is-active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.drawer-content-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.drawer-empty-hint {
  text-align: center;
  padding: 40px 16px;
  color: #94a3b8;
  font-size: 13px;
}

.drawer-media-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.drawer-media-cell {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: #f1f5f9;
  cursor: pointer;
  border: 1px solid #e2e8f0;
}

.drawer-thumb-img,
.drawer-thumb-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.15s ease;
}

.drawer-media-cell:hover .drawer-thumb-img {
  transform: scale(1.06);
}

.video-play-indicator {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  color: #ffffff;
  font-size: 16px;
}

.drawer-files-list,
.drawer-links-list,
.drawer-pinned-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer-file-item,
.drawer-link-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  transition: all 0.15s ease;
}

.drawer-file-item:hover,
.drawer-link-item:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.drawer-file-icon-box,
.drawer-link-icon-box {
  font-size: 18px;
  flex-shrink: 0;
}

.drawer-file-text-col,
.drawer-link-text-col {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.drawer-file-name,
.drawer-link-url {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.drawer-file-meta,
.drawer-link-meta {
  font-size: 11px;
  color: #64748b;
}

.drawer-pinned-item {
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.drawer-pinned-item:hover {
  background: #f1f5f9;
  border-color: #93c5fd;
}

.drawer-pinned-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.drawer-pinned-author {
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
}

.drawer-pinned-time {
  font-size: 10px;
  color: #94a3b8;
}

.drawer-pinned-snippet {
  margin: 0;
  font-size: 12px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* =========================================================================
   THEME COLOR PRESETS GRID
   ========================================================================= */
.theme-picker-desc {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 16px;
}

.theme-presets-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.theme-preset-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.theme-preset-card:hover {
  border-color: #94a3b8;
}

.theme-preset-card.is-selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.theme-swatch-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
}

.theme-preset-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

/* =========================================================================
   SAVED MESSAGES MODAL
   ========================================================================= */
.saved-empty-box {
  text-align: center;
  padding: 40px 16px;
}

.empty-star-icon {
  font-size: 36px;
  margin-bottom: 12px;
}

.saved-empty-box h4 {
  margin: 0 0 6px 0;
  font-size: 16px;
  color: #0f172a;
}

.saved-empty-box p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  max-width: 360px;
  margin: 0 auto;
}

.saved-messages-list-scroll {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 480px;
  overflow-y: auto;
  padding-right: 4px;
}

.saved-message-card {
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.15s ease;
}

.saved-message-card:hover {
  border-color: #cbd5e1;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.saved-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.saved-author-name {
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
}

.saved-time-tag {
  font-size: 11px;
  color: #94a3b8;
}

.saved-content-text {
  margin: 0;
  font-size: 13px;
  color: #1e293b;
  line-height: 1.5;
  white-space: pre-wrap;
}

.saved-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.btn-saved-action {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-saved-action:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-saved-action.is-remove {
  color: #dc2626;
  border-color: #fecaca;
}

.btn-saved-action.is-remove:hover {
  background: #fee2e2;
}

/* =========================================================================
   POLL CREATION MODAL INPUTS
   ========================================================================= */
.poll-options-inputs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.poll-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.poll-opt-index {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  width: 18px;
}

.btn-remove-opt {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 14px;
  padding: 4px;
}

.btn-remove-opt:hover {
  color: #ef4444;
}

.btn-add-poll-option {
  background: transparent;
  border: 1px dashed #cbd5e1;
  color: #2563eb;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  width: 100%;
  transition: all 0.15s ease;
}

.btn-add-poll-option:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.poll-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
}

/* =========================================================================
   RESPONSIVE DESIGN (MÀN HÌNH NHỎ & MOBILE VIEW)
   ========================================================================= */
.btn-mobile-back {
  display: none;
  background: transparent;
  border: none;
  color: #475569;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  margin-right: 6px;
  transition: all 0.15s ease;
}

.btn-mobile-back:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.header-menu-mobile-only {
  display: none;
}

@media (max-width: 900px) {
  .realtime-chat-page {
    padding: 0;
    height: calc(100vh - 64px);
    height: calc(100dvh - 64px);
  }

  .chat-card-container {
    border-radius: 0;
    border: none;
    height: 100%;
  }

  .btn-mobile-back {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: #f1f5f9;
    color: #1e293b;
    margin-right: 8px;
    flex-shrink: 0;
  }

  .chat-sidebar {
    width: 100%;
    height: 100%;
  }

  .chat-sidebar.is-mobile-hidden {
    display: none !important;
  }

  .chat-main {
    width: 100%;
    height: 100%;
  }

  .chat-main.is-mobile-hidden {
    display: none !important;
  }

  .message-bubble-row {
    max-width: 90%;
  }

  .chat-right-drawer {
    position: absolute;
    right: 0;
    top: 0;
    bottom: 0;
    width: 100%;
    z-index: 50;
  }

  .header-dropdown-menu {
    right: 0;
    width: 220px;
  }
}

@media (max-width: 768px) {
  .main-header {
    padding: 8px 10px;
    gap: 8px;
    min-height: 52px;
  }

  .current-room-info {
    min-width: 0;
    gap: 6px;
    flex: 1;
  }

  .header-title-group {
    min-width: 0;
  }

  .current-chat-name {
    font-size: 14px;
    max-width: 140px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .current-chat-desc {
    display: none;
  }

  .presence-status-pill {
    font-size: 10px;
    padding: 1px 6px;
  }

  .header-actions {
    gap: 4px;
    flex-shrink: 0;
  }

  .btn-header-search-toggle,
  .btn-header-drawer-toggle {
    display: none !important;
  }

  .header-menu-mobile-only {
    display: flex !important;
  }

  .btn-header-call,
  .btn-icon-action {
    width: 32px;
    height: 32px;
  }

  .main-footer {
    padding: 6px 8px 10px;
  }

  .composer-unified-card {
    padding: 6px 8px;
    border-radius: 12px;
  }

  .composer-textarea {
    min-height: 34px;
    font-size: 13.5px;
    padding: 2px 2px 4px;
  }

  .composer-bottom-toolbar {
    padding-top: 4px;
  }

  .composer-tools-left {
    gap: 2px;
  }

  .composer-tool-btn {
    width: 30px;
    height: 30px;
  }

  .btn-send-message {
    height: 32px;
    padding: 0 12px;
    font-size: 13px;
    border-radius: 8px;
  }

  .messages-viewport {
    padding: 10px 8px;
  }
}

@media (max-width: 480px) {
  .current-chat-name {
    max-width: 110px;
  }

  .btn-send-message {
    padding: 0 10px;
  }

  .btn-send-message span {
    display: none;
  }

  .message-bubble-row {
    max-width: 95%;
  }

  .composer-tool-divider {
    display: none;
  }
}

/* =========================================================================
   HEADER PRESENCE PILL & CALL BUTTONS
   ========================================================================= */
.presence-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 600;
  padding: 2px 9px;
  border-radius: 999px;
  transition: all 0.2s ease;
  user-select: none;
}

.presence-status-pill.is-online {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.presence-status-pill.is-offline {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
  font-weight: 500;
}

.presence-pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.presence-status-pill.is-online .presence-pulse-dot {
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
  animation: pulse-badge-dot 1.8s infinite;
}

.presence-status-pill.is-offline .presence-pulse-dot {
  background: #94a3b8;
}

@keyframes pulse-badge-dot {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.5); }
  70% { transform: scale(1.1); box-shadow: 0 0 0 5px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.btn-header-call {
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  transition: all 0.2s ease;
}

.btn-header-call:hover {
  background: #2563eb;
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
}

/* =========================================================================
   UNIFIED CHAT COMPOSER (SLACK / TELEGRAM STYLE)
   ========================================================================= */
.chat-composer-container {
  width: 100%;
}

.composer-unified-card {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 12px;
  padding: 6px 12px;
  transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.composer-unified-card.is-focused {
  border-color: var(--chat-theme-primary, #2563eb);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12), 0 3px 8px rgba(0, 0, 0, 0.04);
}

.composer-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.4;
  color: #1e293b;
  background: transparent;
  padding: 2px 4px 4px;
  min-height: 28px;
  max-height: 120px;
}

.composer-bottom-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 4px;
  border-top: 1px solid #f1f5f9;
}

.composer-tools-left {
  display: flex;
  align-items: center;
  gap: 4px;
  position: relative;
}

.composer-tool-btn {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;
}

.composer-tool-btn:hover {
  background: #f1f5f9;
  color: #2563eb;
}

.composer-tool-btn.is-active {
  background: #eff6ff;
  color: #2563eb;
}

.composer-tool-divider {
  width: 1px;
  height: 16px;
  background: #e2e8f0;
  margin: 0 2px;
}

.composer-emoji-wrapper {
  position: relative;
}

.composer-emoji-popover {
  position: absolute;
  bottom: 42px;
  left: 0;
  z-index: 100;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.12);
  padding: 8px;
  width: 250px;
}

.emoji-popover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 6px;
  margin-bottom: 6px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.btn-close-emoji-popover {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 12px;
}

.emoji-popover-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.btn-popover-emoji {
  font-size: 20px;
  padding: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-popover-emoji:hover {
  background: #eff6ff;
  transform: scale(1.15);
}

/* =========================================================================
   WEBRTC CALLING MODALS
   ========================================================================= */
.incoming-call-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.incoming-call-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 32px 28px;
  width: 100%;
  max-width: 360px;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
  animation: pop-in 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.incoming-avatar-sonar-wrapper {
  position: relative;
  width: 90px;
  height: 90px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.incoming-caller-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.incoming-caller-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sonar-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid #10b981;
  animation: sonar-wave 2s infinite ease-out;
}

.sonar-ring.r2 { animation-delay: 0.6s; }
.sonar-ring.r3 { animation-delay: 1.2s; }

@keyframes sonar-wave {
  0% { transform: scale(0.8); opacity: 0.9; }
  100% { transform: scale(2); opacity: 0; }
}

.incoming-caller-name {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6px;
}

.incoming-call-subtitle {
  font-size: 13.5px;
  color: #64748b;
  margin-bottom: 24px;
}

.incoming-call-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.btn-call-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}

.btn-call-action svg {
  width: 52px;
  height: 52px;
  padding: 13px;
  border-radius: 50%;
  color: #fff;
  transition: all 0.2s;
}

.btn-call-decline svg {
  background: #ef4444;
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.4);
}

.btn-call-accept svg {
  background: #10b981;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.4);
  animation: pulse-accept 1.5s infinite;
}

@keyframes pulse-accept {
  0% { transform: scale(1); }
  50% { transform: scale(1.08); }
  100% { transform: scale(1); }
}

/* Active Call Overlay */
.active-call-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background: rgba(10, 15, 29, 0.92);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.active-call-stage {
  width: 100%;
  max-width: 900px;
  height: 85vh;
  max-height: 640px;
  background: #0f172a;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.5);
}

.call-stage-header {
  position: absolute;
  top: 16px;
  left: 20px;
  right: 20px;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  pointer-events: none;
}

.call-header-info h4 {
  font-size: 17px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 4px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.6);
}

.active-timer-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #ffffff;
}

.live-red-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ef4444;
  animation: blink-dot 1s infinite;
}

.pulse-call-text {
  color: #38bdf8;
  font-size: 13px;
  font-weight: 600;
  animation: blink-dot 1.2s infinite;
}

.call-video-grid {
  flex: 1;
  position: relative;
  width: 100%;
  height: 100%;
  background: #020617;
}

.remote-video-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.remote-video-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-placeholder-cover {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #ffffff;
}

.large-avatar-call {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  border: 3px solid rgba(255, 255, 255, 0.4);
}

.wait-cam-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.local-pip-container {
  position: absolute;
  bottom: 90px;
  right: 20px;
  width: 160px;
  height: 110px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.4);
  background: #1e293b;
  z-index: 5;
}

.local-video-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
}

.pip-cam-off-badge {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1e293b;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
}

.call-audio-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: radial-gradient(circle at center, #1e293b 0%, #0f172a 100%);
}

.audio-caller-avatar-circle {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 42px;
  font-weight: 700;
  position: relative;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.4);
}

.audio-caller-avatar-circle img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.audio-soundwaves-ring .sound-wave {
  position: absolute;
  inset: -12px;
  border-radius: 50%;
  border: 2px solid rgba(56, 189, 248, 0.6);
  animation: soundwave-pulse 2s infinite ease-out;
}

.audio-soundwaves-ring .sw2 {
  inset: -24px;
  animation-delay: 0.8s;
}

@keyframes soundwave-pulse {
  0% { transform: scale(0.9); opacity: 0.8; }
  100% { transform: scale(1.3); opacity: 0; }
}

.audio-call-note {
  font-size: 13px;
  color: #94a3b8;
}

.call-controls-bar {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(8px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 10;
}

.btn-call-ctl {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-call-ctl:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.08);
}

.btn-call-ctl.is-active-off {
  background: #f59e0b;
  color: #ffffff;
}

.btn-call-ctl.btn-hangup {
  width: 56px;
  height: 56px;
  background: #ef4444;
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.5);
}

.btn-call-ctl.btn-hangup:hover {
  background: #dc2626;
  transform: scale(1.1);
}

/* =========================================================================
   MODAL CÀI ĐẶT NHẠC CHUÔNG CUỘC GỌI
   ========================================================================= */
.ringtone-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.ringtone-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ringtone-item-row:hover {
  border-color: #93c5fd;
  background: #f0f7ff;
}

.ringtone-item-row.is-selected {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
}

.ringtone-radio-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.ringtone-item-row.is-selected .ringtone-radio-dot {
  border-color: #2563eb;
}

.radio-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2563eb;
}

.ringtone-info {
  flex: 1;
}

.ringtone-name {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 2px;
}

.ringtone-desc {
  font-size: 12px;
  color: #64748b;
}

.btn-preview-ringtone {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s ease;
}

.btn-preview-ringtone:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

.btn-preview-ringtone.is-playing {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #dc2626;
  animation: pulsePreviewBtn 1s infinite alternate;
}

@keyframes pulsePreviewBtn {
  0% { transform: scale(1); }
  100% { transform: scale(1.04); }
}

.ringtone-volume-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.volume-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
}

.ringtone-vol-slider {
  flex: 1;
  cursor: pointer;
  accent-color: #2563eb;
}

.volume-val {
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
  width: 42px;
  text-align: right;
}

</style>

