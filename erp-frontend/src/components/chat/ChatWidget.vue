<template>
  <div class="chat-widget-wrapper">
    <!-- 1. FLOATING ACTION BUTTON (Flat style, không đổ bóng, viền sắc nét) -->
    <div 
      class="chat-floating-btn" 
      :class="{ 'is-active': isOpen, 'btn-dark': isDarkMode }" 
      @click="toggleWidget"
      title="Trợ lý ảo ERP AI"
    >
      <div class="btn-inner">
        <el-icon v-if="!isOpen" class="btn-icon"><ChatDotRound /></el-icon>
        <el-icon v-else class="btn-icon"><Close /></el-icon>
        <span class="online-indicator" title="AI đang trực tuyến"></span>
      </div>
    </div>

    <!-- 2. CỬA SỔ HỘI THOẠI CHATBOT (Flat Clean Window, KHÔNG ĐỔ BÓNG) -->
    <transition name="chat-popup">
      <div 
        v-if="isOpen" 
        class="chat-window"
        :class="isDarkMode ? 'theme-dark' : 'theme-light'"
      >
        <!-- HEADER -->
        <div class="chat-header">
          <div class="header-left">
            <div class="bot-avatar-wrap">
              <el-avatar :size="36" class="bot-avatar">
                <el-icon><Cpu /></el-icon>
              </el-avatar>
              <span class="avatar-status-dot"></span>
            </div>
            <div class="bot-info">
              <div class="bot-title">Trợ lý ảo ERP AI</div>
              <div class="bot-status">
                <span class="status-dot">●</span>
                <span class="status-text">Trực tuyến</span>
                <span class="status-sep">•</span>
                <span class="status-desc">Tra cứu ERP</span>
              </div>
            </div>
          </div>

          <div class="header-actions">
            <!-- Nút chuyển đổi nhanh Nền Đậm (Dark) / Nền Sáng (Light) -->
            <button 
              class="header-btn" 
              :title="isDarkMode ? 'Chuyển sang nền sáng' : 'Chuyển sang nền đậm'"
              @click="toggleTheme"
            >
              <el-icon><Sunny v-if="isDarkMode" /><Moon v-else /></el-icon>
            </button>

            <!-- Nút xóa lịch sử -->
            <button 
              class="header-btn" 
              title="Xóa toàn bộ lịch sử trò chuyện" 
              @click="handleClearHistory"
              :disabled="loading || isStreaming || messages.length === 0"
            >
              <el-icon><Delete /></el-icon>
            </button>

            <!-- Nút thu nhỏ -->
            <button class="header-btn" title="Thu nhỏ" @click="isOpen = false">
              <el-icon><Minus /></el-icon>
            </button>
          </div>
        </div>

        <!-- BODY: MESSAGE FEED -->
        <div class="chat-body" ref="messagesContainerRef">
          <!-- Welcome message if no chat history yet -->
          <div v-if="messages.length === 0 && !fetchingHistory" class="chat-welcome-card">
            <div class="welcome-icon">
              <el-icon><Opportunity /></el-icon>
            </div>
            <h4 class="welcome-title">Xin chào {{ userDisplayName }}! 👋</h4>
            <p class="welcome-desc">
              Tôi là <strong>Trợ lý ảo ERP AI</strong>. Tôi có thể hỗ trợ bạn tra cứu trực tiếp kho hàng, tài khoản người dùng và thông báo toàn hệ thống.
            </p>

            <!-- Quick Action Chips -->
            <div class="quick-chips-wrap">
              <div class="chips-label">Gợi ý câu hỏi nhanh:</div>
              <div class="chips-list">
                <button 
                  v-for="(chip, idx) in quickPrompts" 
                  :key="idx" 
                  class="quick-chip-btn"
                  @click="sendQuickPrompt(chip)"
                >
                  {{ chip }}
                </button>
              </div>
            </div>
          </div>

          <!-- Loading state when fetching history -->
          <div v-if="fetchingHistory" class="chat-loading-history">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>Đang tải lịch sử trò chuyện...</span>
          </div>

          <!-- Message List -->
          <div v-else class="message-stream">
            <div 
              v-for="(msg, index) in messages" 
              :key="msg.id || index" 
              :class="['message-row', msg.role === 'USER' ? 'row-user' : 'row-assistant']"
            >
              <!-- Assistant Avatar -->
              <div v-if="msg.role === 'ASSISTANT'" class="msg-avatar assistant-avatar">
                <el-icon><Cpu /></el-icon>
              </div>

              <!-- Message Bubble -->
              <div class="msg-bubble-wrap">
                <div class="msg-bubble">
                  <!-- Formatted HTML text support for bold, bullet points, line breaks -->
                  <div class="msg-text" v-html="formatMessage(msg.content)"></div>
                  <!-- Hiệu ứng con trỏ nhấp nháy khi đang gõ từng chữ (typing streaming) -->
                  <span v-if="msg.isTyping" class="typewriter-cursor">▌</span>
                </div>
                <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
              </div>

              <!-- User Avatar -->
              <div v-if="msg.role === 'USER'" class="msg-avatar user-avatar">
                {{ userInitial }}
              </div>
            </div>

            <!-- Typing Indicator (Khi bot đang suy nghĩ và tính toán) -->
            <div v-if="loading" class="message-row row-assistant">
              <div class="msg-avatar assistant-avatar">
                <el-icon><Cpu /></el-icon>
              </div>
              <div class="msg-bubble-wrap">
                <div class="msg-bubble typing-bubble">
                  <div class="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <span class="typing-text">Trợ lý AI đang suy nghĩ...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- FOOTER: INPUT BAR -->
        <div class="chat-footer">
          <!-- Suggestion pills when history exists -->
          <div v-if="messages.length > 0" class="mini-chips-scroll">
            <button 
              v-for="(chip, idx) in quickPrompts" 
              :key="'mini-' + idx" 
              class="mini-chip"
              :disabled="loading || isStreaming"
              @click="sendQuickPrompt(chip)"
            >
              {{ chip }}
            </button>
          </div>

          <div class="input-form">
            <el-input 
              v-model="inputMessage" 
              placeholder="Hỏi bất kỳ điều gì về hệ thống ERP..." 
              :disabled="loading || isStreaming"
              clearable
              class="chat-input"
              @keyup.enter="handleSend"
            />
            <button 
              type="button" 
              class="btn-send" 
              :disabled="loading || isStreaming || !inputMessage.trim()"
              @click="handleSend"
              title="Gửi tin nhắn"
            >
              <el-icon><Promotion /></el-icon>
            </button>
          </div>

          <div class="footer-note">
            <span>Tài khoản: <strong>@{{ authStore.user?.username }}</strong> • Phân lập dữ liệu độc lập</span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { useAuthStore } from '../../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'
import { 
  ChatDotRound, 
  Close, 
  Minus, 
  Delete, 
  Promotion, 
  Cpu, 
  Opportunity, 
  Loading,
  Sunny,
  Moon
} from '@element-plus/icons-vue'

const authStore = useAuthStore()

// State điều khiển mở/đóng và theme
const isOpen = ref(false)
const inputMessage = ref('')
const messages = ref([])
const loading = ref(false) // Hiệu ứng suy nghĩ (dots)
const isStreaming = ref(false) // Hiệu ứng gõ từng chữ (typing streaming)
const fetchingHistory = ref(false)
const messagesContainerRef = ref(null)

// Theme nền: Mặc định là Dark Mode (Màu nền đậm phẳng, không đổ bóng)
const isDarkMode = ref(localStorage.getItem('erp_chat_theme') !== 'light')

const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  localStorage.setItem('erp_chat_theme', isDarkMode.value ? 'dark' : 'light')
}

const quickPrompts = [
  '📦 Kiểm tra tồn kho sản phẩm',
  '👥 Thống kê tài khoản người dùng',
  '📢 Xem thông báo hệ thống',
  '❓ Chào hỏi và trợ giúp'
]

const userDisplayName = computed(() => {
  return authStore.user?.fullName || authStore.user?.username || 'Bạn'
})

const userInitial = computed(() => {
  const name = authStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

// Cuộn mượt xuống cuối tin nhắn
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
  }
}

// Format markdown cơ bản
const formatMessage = (rawText) => {
  if (!rawText) return ''
  let escaped = rawText
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // In đậm: **text** -> <strong>text</strong>
  escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  // In nghiêng: *text* -> <em>text</em>
  escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>')
  // Dòng xuống hàng
  escaped = escaped.replace(/\n/g, '<br>')
  return escaped
}

const formatTime = (timeStr) => {
  if (!timeStr) {
    const now = new Date()
    return now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  }
  try {
    const d = new Date(timeStr)
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  } catch (e) {
    return ''
  }
}

// Tải lịch sử trò chuyện ĐỘC LẬP của tài khoản đang đăng nhập
const fetchHistory = async () => {
  if (!authStore.isAuthenticated) return
  fetchingHistory.value = true
  try {
    const res = await api.get('/chat/history')
    messages.value = res.data || []
    scrollToBottom()
  } catch (err) {
    console.error('Lỗi khi tải lịch sử chat:', err)
  } finally {
    fetchingHistory.value = false
  }
}

// Bật / Tắt widget
const toggleWidget = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value && messages.value.length === 0) {
    fetchHistory()
  }
  if (isOpen.value) {
    scrollToBottom()
  }
}

// Gửi câu hỏi nhanh
const sendQuickPrompt = (promptText) => {
  if (loading.value || isStreaming.value) return
  inputMessage.value = promptText
  handleSend()
}

// Helper tạm dừng (delay ms)
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

// Gửi tin nhắn với hiệu ứng delay thinking và typing streaming mượt mà
const handleSend = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value || isStreaming.value) return

  // 1. Thêm tin nhắn của User lên giao diện ngay lập tức
  const tempUserMsg = {
    role: 'USER',
    content: content,
    createdAt: new Date().toISOString()
  }
  messages.value.push(tempUserMsg)
  inputMessage.value = ''
  
  // 2. Bắt đầu trạng thái suy nghĩ với hiệu ứng 3 dấu chấm nhấp nháy
  loading.value = true
  scrollToBottom()

  try {
    // Thêm độ trễ suy nghĩ tối thiểu (~750ms) để người dùng cảm nhận AI đang tính toán tra cứu DB
    const minThinkingDelay = sleep(750)
    const apiCall = api.post('/chat/send', { message: content })
    const [_, res] = await Promise.all([minThinkingDelay, apiCall])

    // 3. Tắt indicator suy nghĩ, bắt đầu hiệu ứng gõ từng chữ (Typewriter Effect)
    loading.value = false
    const fullText = res.data?.content || ''

    // Tạo tin nhắn trợ lý rỗng và gõ dần vào
    const botMsg = reactive({
      id: res.data?.id || Date.now(),
      role: 'ASSISTANT',
      content: '',
      createdAt: res.data?.createdAt || new Date().toISOString(),
      isTyping: true
    })
    messages.value.push(botMsg)
    isStreaming.value = true

    // Thuật toán gõ chữ mượt mà: gõ theo cụm 2-4 ký tự mỗi 18ms
    const step = Math.max(1, Math.min(4, Math.floor(fullText.length / 40)))
    for (let i = 0; i < fullText.length; i += step) {
      botMsg.content = fullText.slice(0, i + step)
      scrollToBottom()
      await sleep(18)
    }

    // Đảm bảo gõ trọn vẹn văn bản cuối cùng
    botMsg.content = fullText
    botMsg.isTyping = false
    isStreaming.value = false
    scrollToBottom()

  } catch (err) {
    loading.value = false
    isStreaming.value = false
    messages.value.push({
      role: 'ASSISTANT',
      content: '⚠️ Xin lỗi, đã có lỗi kết nối với máy chủ AI. Vui lòng thử lại sau!',
      createdAt: new Date().toISOString()
    })
    scrollToBottom()
  }
}

// Xóa sạch lịch sử trò chuyện của tài khoản này
const handleClearHistory = async () => {
  try {
    await ElMessageBox.confirm(
      'Bạn có chắc chắn muốn xóa toàn bộ lịch sử trò chuyện của tài khoản này?',
      'Xóa lịch sử chat',
      {
        confirmButtonText: 'Xóa ngay',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }
    )

    await api.delete('/chat/history')
    messages.value = []
    ElMessage.success('Đã xóa sạch lịch sử trò chuyện của bạn!')
  } catch (e) {
    // User cancelled
  }
}

// THEO DÕI: Nếu người dùng đổi tài khoản hoặc đăng xuất, lập tức xóa sạch state cũ và tải lại lịch sử mới
watch(() => authStore.user?.id, (newId, oldId) => {
  if (newId !== oldId) {
    messages.value = []
    if (isOpen.value) {
      fetchHistory()
    }
  }
})

onMounted(() => {
  if (authStore.isAuthenticated) {
    fetchHistory()
  }
})
</script>

<style scoped>
.chat-widget-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
  font-family: inherit;
}

/* ========================================================
   1. FLOATING ACTION BUTTON (PHẲNG - HOÀN TOÀN KHÔNG ĐỔ BÓNG)
   ======================================================== */
.chat-floating-btn {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: #1e293b;
  border: 2px solid #38bdf8;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #38bdf8;
  cursor: pointer;
  box-shadow: none !important;
  transition: all 0.25s ease;
  position: relative;
  user-select: none;
}

.chat-floating-btn:hover {
  transform: scale(1.05);
  background: #0f172a;
  border-color: #60a5fa;
  color: #ffffff;
  box-shadow: none !important;
}

.chat-floating-btn.is-active {
  transform: rotate(90deg);
  background: #0f172a;
  border-color: #ef4444;
  color: #ef4444;
}

.btn-inner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon {
  font-size: 24px;
}

.online-indicator {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 11px;
  height: 11px;
  background-color: #10b981;
  border: 2px solid #1e293b;
  border-radius: 50%;
}

/* ========================================================
   2. CHAT WINDOW (FLAT LAYOUT - KHÔNG ĐỔ BÓNG)
   ======================================================== */
.chat-window {
  position: absolute;
  bottom: 68px;
  right: 0;
  width: 390px;
  height: 560px;
  max-height: calc(100vh - 110px);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: none !important;
  animation: popup-flat 0.22s ease-out;
}

/* ---------------- THEME: NỀN ĐẬM (DARK MODE - DEFAULT) ---------------- */
.chat-window.theme-dark {
  background: #0f172a;
  border: 1px solid #334155;
  color: #f8fafc;
}

.theme-dark .chat-header {
  background: #1e293b;
  border-bottom: 1px solid #334155;
  color: #ffffff;
}

.theme-dark .chat-body {
  background-color: #0b1120;
}

.theme-dark .chat-welcome-card {
  background: #1e293b;
  border: 1px solid #334155;
  box-shadow: none !important;
}

.theme-dark .welcome-icon {
  background: #0f172a;
  color: #38bdf8;
  border: 1px solid #334155;
}

.theme-dark .welcome-title {
  color: #f8fafc;
}

.theme-dark .welcome-desc {
  color: #94a3b8;
}

.theme-dark .chips-label {
  color: #cbd5e1;
}

.theme-dark .quick-chip-btn {
  background: #0f172a;
  border: 1px solid #334155;
  color: #e2e8f0;
}

.theme-dark .quick-chip-btn:hover {
  background: #334155;
  border-color: #38bdf8;
  color: #38bdf8;
}

.theme-dark .row-assistant .msg-bubble {
  background: #1e293b;
  border: 1px solid #334155;
  color: #f1f5f9;
  box-shadow: none !important;
}

.theme-dark .row-user .msg-bubble {
  background: #2563eb;
  border: 1px solid #3b82f6;
  color: #ffffff;
  box-shadow: none !important;
}

.theme-dark .typing-bubble {
  background: #1e293b;
  border: 1px solid #334155;
  box-shadow: none !important;
}

.theme-dark .typing-text {
  color: #94a3b8;
}

.theme-dark .chat-footer {
  background: #1e293b;
  border-top: 1px solid #334155;
}

.theme-dark .mini-chip {
  background: #0f172a;
  border: 1px solid #334155;
  color: #94a3b8;
}

.theme-dark .mini-chip:hover {
  background: #334155;
  color: #38bdf8;
  border-color: #38bdf8;
}

.theme-dark .chat-input :deep(.el-input__wrapper) {
  background-color: #0f172a;
  border: 1px solid #334155;
  box-shadow: none !important;
  color: #ffffff;
}

.theme-dark .chat-input :deep(.el-input__inner) {
  color: #ffffff;
}

.theme-dark .chat-input :deep(.el-input__wrapper.is-focus) {
  border-color: #38bdf8;
}

.theme-dark .footer-note {
  color: #64748b;
}

/* ---------------- THEME: NỀN SÁNG (LIGHT MODE) ---------------- */
.chat-window.theme-light {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #0f172a;
}

.theme-light .chat-header {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  color: #0f172a;
}

.theme-light .chat-body {
  background-color: #f1f5f9;
}

.theme-light .chat-welcome-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: none !important;
}

.theme-light .welcome-icon {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.theme-light .welcome-title {
  color: #0f172a;
}

.theme-light .welcome-desc {
  color: #64748b;
}

.theme-light .chips-label {
  color: #475569;
}

.theme-light .quick-chip-btn {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #1e293b;
}

.theme-light .quick-chip-btn:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.theme-light .row-assistant .msg-bubble {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #1e293b;
  box-shadow: none !important;
}

.theme-light .row-user .msg-bubble {
  background: #2563eb;
  border: 1px solid #1d4ed8;
  color: #ffffff;
  box-shadow: none !important;
}

.theme-light .typing-bubble {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: none !important;
}

.theme-light .typing-text {
  color: #64748b;
}

.theme-light .chat-footer {
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}

.theme-light .mini-chip {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
}

.theme-light .mini-chip:hover {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
}

.theme-light .chat-input :deep(.el-input__wrapper) {
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  box-shadow: none !important;
  color: #1e293b;
}

.theme-light .chat-input :deep(.el-input__inner) {
  color: #1e293b;
}

.theme-light .chat-input :deep(.el-input__wrapper.is-focus) {
  border-color: #2563eb;
}

.theme-light .footer-note {
  color: #94a3b8;
}

/* ========================================================
   HEADER CHUNG
   ======================================================== */
.chat-header {
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bot-avatar-wrap {
  position: relative;
}

.bot-avatar {
  background: #2563eb;
  color: #ffffff;
  font-size: 17px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.avatar-status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 9px;
  height: 9px;
  background: #10b981;
  border: 2px solid #1e293b;
  border-radius: 50%;
}

.bot-info {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.bot-title {
  font-weight: 700;
  font-size: 14px;
  letter-spacing: 0.1px;
}

.bot-status {
  font-size: 11px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
}

.status-dot {
  color: #10b981;
  font-size: 8px;
}

.status-text {
  color: #10b981;
  font-weight: 600;
}

.status-sep {
  color: #64748b;
}

.status-desc {
  color: #94a3b8;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-btn {
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: inherit;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  box-shadow: none !important;
}

.header-btn:hover:not(:disabled) {
  background: rgba(148, 163, 184, 0.15);
  border-color: rgba(148, 163, 184, 0.4);
}

.header-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ========================================================
   BODY: MESSAGES
   ======================================================== */
.chat-body {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-welcome-card {
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  margin-top: 4px;
}

.welcome-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin: 0 auto 10px auto;
}

.welcome-title {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 6px 0;
}

.welcome-desc {
  font-size: 12px;
  margin: 0 0 14px 0;
  line-height: 1.45;
}

.quick-chips-wrap {
  text-align: left;
}

.chips-label {
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 6px;
}

.chips-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-chip-btn {
  border-radius: 8px;
  padding: 7px 10px;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
  font-weight: 500;
  box-shadow: none !important;
}

.chat-loading-history {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  color: #94a3b8;
  font-size: 12px;
}

/* Message Stream */
.message-stream {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 90%;
}

.row-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.row-assistant {
  align-self: flex-start;
}

.msg-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}

.assistant-avatar {
  background: #2563eb;
  color: white;
}

.user-avatar {
  background: #0284c7;
  color: white;
  font-size: 11px;
  font-weight: 700;
}

.msg-bubble-wrap {
  display: flex;
  flex-direction: column;
}

.row-user .msg-bubble-wrap {
  align-items: flex-end;
}

.row-assistant .msg-bubble-wrap {
  align-items: flex-start;
}

.msg-bubble {
  padding: 9px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.45;
  word-break: break-word;
}

.row-user .msg-bubble {
  border-bottom-right-radius: 2px;
}

.row-assistant .msg-bubble {
  border-bottom-left-radius: 2px;
}

.msg-time {
  font-size: 10px;
  color: #64748b;
  margin-top: 2px;
  padding: 0 4px;
}

/* Typewriter blinking cursor */
.typewriter-cursor {
  display: inline-block;
  color: #38bdf8;
  font-weight: 700;
  animation: cursor-blink 0.8s infinite;
  margin-left: 2px;
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Typing Indicator (Dots) */
.typing-bubble {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 12px;
}

.typing-dots {
  display: flex;
  align-items: center;
  gap: 4px;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  background-color: #38bdf8;
  border-radius: 50%;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots span:nth-child(2) { animation-delay: -0.16s; }
.typing-dots span:nth-child(3) { animation-delay: 0s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

.typing-text {
  font-size: 11px;
  font-style: italic;
}

/* ========================================================
   FOOTER
   ======================================================== */
.chat-footer {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.mini-chips-scroll {
  display: flex;
  align-items: center;
  gap: 5px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;
}

.mini-chips-scroll::-webkit-scrollbar {
  display: none;
}

.mini-chip {
  border-radius: 14px;
  padding: 3px 8px;
  font-size: 11px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.18s;
  flex-shrink: 0;
  box-shadow: none !important;
}

.mini-chip:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-form {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-input :deep(.el-input__wrapper) {
  border-radius: 18px;
  padding: 4px 12px;
}

.btn-send {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #2563eb;
  border: 1px solid #3b82f6;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  box-shadow: none !important;
}

.btn-send:hover:not(:disabled) {
  background: #1d4ed8;
  border-color: #60a5fa;
  transform: scale(1.04);
}

.btn-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

.footer-note {
  font-size: 10px;
  text-align: center;
}

/* Flat Popup Animation (Không rung giật, nhẹ nhàng) */
.chat-popup-enter-active,
.chat-popup-leave-active {
  transition: all 0.2s ease;
}

.chat-popup-enter-from,
.chat-popup-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.97);
}

@keyframes popup-flat {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 768px) {
  .chat-widget-wrapper {
    bottom: 16px;
    right: 16px;
  }

  .chat-window {
    width: calc(100vw - 32px);
    right: 0;
    bottom: 64px;
    height: 500px;
  }
}
</style>
