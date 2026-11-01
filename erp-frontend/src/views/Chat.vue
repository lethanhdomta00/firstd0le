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
              <p class="item-preview">Thảo luận công việc & thông báo nhanh</p>
            </div>
            <span v-if="chatStore.unreadCounts['GLOBAL']" class="unread-pill">
              {{ chatStore.unreadCounts['GLOBAL'] }}
            </span>
          </div>

          <!-- PHẦN 2: TIN NHẮN RIÊNG 1-1 (ĐỒNG NGHIỆP) -->
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
                <span class="item-name">{{ colleague.fullName || colleague.username }}</span>
                <span class="role-badge" :class="colleague.role === 'ADMIN' ? 'role-admin' : 'role-user'">
                  {{ colleague.role === 'ADMIN' ? 'ADMIN' : 'STAFF' }}
                </span>
              </div>
              <p class="item-preview">@{{ colleague.username }}</p>
            </div>

            <span 
              v-if="getPrivateUnreadCount(colleague.username)" 
              class="unread-pill"
            >
              {{ getPrivateUnreadCount(colleague.username) }}
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
                <span v-if="chatStore.activeChatType === 'PUBLIC'" class="badge-tag-channel">Phòng mở</span>
                <span v-else class="badge-tag-private">Bảo mật 1-1</span>
              </div>
              <p class="current-chat-desc">{{ activeSubtitle }}</p>
            </div>
          </div>

          <!-- Nút hành động nhanh trên Header -->
          <div class="header-actions">
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
          
          <!-- Thông báo chào mừng đầu phòng -->
          <div class="room-welcome-banner">
            <div class="welcome-icon-box">
              <svg v-if="chatStore.activeChatType === 'PUBLIC'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </div>
            <h4 class="welcome-title">{{ chatStore.activeChatType === 'PUBLIC' ? 'Chào mừng đến Kênh chung Doanh nghiệp!' : `Cuộc trò chuyện với ${activeTitle}` }}</h4>
            <p class="welcome-desc">
              {{ chatStore.activeChatType === 'PUBLIC' 
                ? 'Đây là không gian trao đổi, phối hợp công việc thời gian thực giữa các phòng ban.' 
                : 'Tin nhắn riêng tư giữa hai đồng nghiệp được đồng bộ thời gian thực qua giao thức WebSocket STOMP.' }}
            </p>
          </div>

          <!-- Danh sách bong bóng tin nhắn -->
          <div 
            v-for="(msg, index) in chatStore.currentMessages" 
            :key="msg.id || index"
            class="message-bubble-row"
            :class="isMyMessage(msg) ? 'is-outgoing' : 'is-incoming'"
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
              <!-- Tên người gửi (khi trong phòng chung) -->
              <div v-if="!isMyMessage(msg) && chatStore.activeChatType === 'PUBLIC'" class="sender-name-row">
                <span class="sender-name">{{ msg.senderFullName || msg.senderUsername }}</span>
                <span class="sender-username">@{{ msg.senderUsername }}</span>
              </div>

              <!-- Bong bóng chứa nội dung văn bản -->
              <div class="message-bubble">
                <div class="bubble-text">{{ msg.content }}</div>
                
                <!-- Thời gian & trạng thái gửi -->
                <div class="bubble-meta">
                  <span class="meta-time">{{ formatTime(msg.createdAt) }}</span>
                  <span v-if="isMyMessage(msg)" class="meta-status-check">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
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

          <form class="chat-input-form" @submit.prevent="handleSendMessage">
            <textarea
              ref="inputTextareaRef"
              v-model="inputContent"
              class="chat-textarea"
              placeholder="Nhập nội dung tin nhắn... (Nhấn Enter để gửi, Shift+Enter xuống dòng)"
              rows="2"
              @keydown.enter.exact.prevent="handleSendMessage"
              @input="handleTyping"
            ></textarea>

            <button 
              type="submit" 
              class="btn-send-message"
              :disabled="!inputContent.trim() || !chatStore.isConnected"
            >
              <span>Gửi</span>
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            </button>
          </form>
        </footer>

      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
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
  return chatStore.activePartner?.fullName || chatStore.activePartner?.username || 'Đồng nghiệp'
})

const activeSubtitle = computed(() => {
  if (chatStore.activeChatType === 'PUBLIC') {
    return 'Kênh phát tin công khai • Kết nối toàn bộ nhân sự doanh nghiệp'
  }
  return `@${chatStore.activePartner?.username || ''} • Tin nhắn riêng tư 1-1`
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

// Gửi tin nhắn
const handleSendMessage = () => {
  const text = inputContent.value.trim()
  if (!text || !chatStore.isConnected) return

  if (chatStore.activeChatType === 'PUBLIC') {
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

// Xử lý sự kiện scroll để hiển thị nút Scroll-to-bottom
const handleScroll = () => {
  if (!messagesScrollRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = messagesScrollRef.value
  const distanceFromBottom = scrollHeight - scrollTop - clientHeight
  showScrollToBottom.value = distanceFromBottom > 150
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

// Watch số lượng tin nhắn để tự động cuộn
watch(
  () => chatStore.currentMessages.length,
  () => {
    scrollToBottom(true)
  }
)

onMounted(() => {
  // Đảm bảo WebSocket STOMP đã kích hoạt
  chatStore.connect()
  // Tải danh bạ đồng nghiệp
  chatStore.fetchColleagues()
  // Mở mặc định phòng chung
  selectPublicRoom()
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

.unread-pill {
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 12px;
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
}

.btn-icon-action:hover {
  background: #f1f5f9;
  color: #0f172a;
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
</style>

