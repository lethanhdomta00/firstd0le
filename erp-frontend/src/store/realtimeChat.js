import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import api from '../api'
import { useAuthStore } from './auth'

export const useRealtimeChatStore = defineStore('realtimeChat', {
  state: () => ({
    client: null,
    isConnected: false,
    isConnecting: false,

    // Phòng chat hoặc đối tượng đang mở
    activeRoomId: 'GLOBAL', // 'GLOBAL' hoặc 'user1_user2'
    activeChatType: 'PUBLIC', // 'PUBLIC' | 'PRIVATE'
    activePartner: null, // UserResponseDto khi chat 1-1

    // Tin nhắn theo phòng: { [roomId]: [RealtimeMessageDto] }
    messagesByRoom: {},

    // Số tin chưa đọc theo phòng: { [roomId]: number }
    unreadCounts: {},
    totalUnreadCount: 0,

    // Danh sách đồng nghiệp để chat 1-1
    colleagues: [],

    // Ai đang gõ phím: { [roomId]: Array<string> }
    typingUsers: {},

    // Subscriptions để quản lý hủy khi disconnect
    subscriptions: []
  }),

  getters: {
    currentMessages: (state) => state.messagesByRoom[state.activeRoomId] || [],
    currentTypingUsers: (state) => state.typingUsers[state.activeRoomId] || []
  },

  actions: {
    /**
     * Khởi tạo và kích hoạt kết nối WebSocket STOMP
     */
    connect() {
      const authStore = useAuthStore()
      if (!authStore.token || this.isConnected || this.isConnecting) {
        return
      }

      this.isConnecting = true

      // Lấy URL WebSocket (hỗ trợ cả dev proxy và production domain)
      const wsUrl = (import.meta.env.VITE_API_URL 
        ? import.meta.env.VITE_API_URL.replace(/\/api\/?$/, '') 
        : window.location.origin) + '/ws-chat'

      this.client = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        connectHeaders: {
          Authorization: `Bearer ${authStore.token}`
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: () => {
          // Bật console khi cần debug luồng frame STOMP
        },
        onConnect: () => {
          this.isConnected = true
          this.isConnecting = false
          this.subscribeChannels()
          this.fetchTotalUnreadCount()
          this.fetchColleagues()
          // Tải trước dữ liệu phòng hiện tại
          if (this.activeChatType === 'PUBLIC') {
            this.loadRoomHistory('GLOBAL')
          }
        },
        onDisconnect: () => {
          this.isConnected = false
          this.isConnecting = false
          this.subscriptions = []
        },
        onStompError: (frame) => {
          console.error('STOMP Broker Error:', frame.headers['message'])
          this.isConnected = false
          this.isConnecting = false
        }
      })

      this.client.activate()
    },

    /**
     * Đăng ký lắng nghe các kênh STOMP
     */
    subscribeChannels() {
      if (!this.client || !this.isConnected) return

      // Hủy subscriptions cũ nếu có
      this.subscriptions.forEach(sub => {
        try { sub.unsubscribe() } catch (e) {}
      })
      this.subscriptions = []

      // 1. Kênh phòng chat chung toàn công ty
      const publicSub = this.client.subscribe('/topic/room.GLOBAL', (message) => {
        try {
          const payload = JSON.parse(message.body)
          this.handleIncomingMessage('GLOBAL', payload)
        } catch (e) {
          console.error('Lỗi parse tin nhắn public:', e)
        }
      })
      this.subscriptions.push(publicSub)

      // 2. Kênh thông báo trạng thái đang gõ phím phòng chung
      const publicTypingSub = this.client.subscribe('/topic/room.GLOBAL.typing', (message) => {
        try {
          const payload = JSON.parse(message.body)
          this.handleTypingIndicator('GLOBAL', payload)
        } catch (e) {
          console.error('Lỗi parse typing public:', e)
        }
      })
      this.subscriptions.push(publicTypingSub)

      // 3. Kênh nhận tin nhắn riêng 1-1 gửi đến đích danh user hiện tại
      const privateSub = this.client.subscribe('/user/queue/messages', (message) => {
        try {
          const payload = JSON.parse(message.body)
          const roomId = payload.roomId || this.getPrivateRoomId(payload.senderUsername, payload.recipientUsername)
          this.handleIncomingMessage(roomId, payload)
        } catch (e) {
          console.error('Lỗi parse tin nhắn private:', e)
        }
      })
      this.subscriptions.push(privateSub)

      // 4. Kênh nhận trạng thái đang gõ phím riêng 1-1
      const privateTypingSub = this.client.subscribe('/user/queue/typing', (message) => {
        try {
          const payload = JSON.parse(message.body)
          const authStore = useAuthStore()
          const myUsername = authStore.user?.username
          const otherUsername = payload.senderUsername === myUsername ? payload.recipientUsername : payload.senderUsername
          const roomId = this.getPrivateRoomId(myUsername, otherUsername)
          this.handleTypingIndicator(roomId, payload)
        } catch (e) {
          console.error('Lỗi parse typing private:', e)
        }
      })
      this.subscriptions.push(privateTypingSub)
    },

    /**
     * Xử lý khi có tin nhắn mới tới
     */
    handleIncomingMessage(roomId, messageDto) {
      if (!this.messagesByRoom[roomId]) {
        this.messagesByRoom[roomId] = []
      }

      // Tránh trùng lặp tin nhắn theo ID
      const exists = this.messagesByRoom[roomId].some(m => m.id === messageDto.id)
      if (!exists) {
        this.messagesByRoom[roomId].push(messageDto)
      }

      const authStore = useAuthStore()
      const isMyMessage = messageDto.senderUsername === authStore.user?.username

      // Nếu đang mở đúng phòng này
      if (this.activeRoomId === roomId) {
        if (!isMyMessage) {
          this.markAsRead(roomId)
        }
      } else {
        // Tăng badge số tin chưa đọc cho phòng này
        if (!isMyMessage) {
          this.unreadCounts[roomId] = (this.unreadCounts[roomId] || 0) + 1
          this.totalUnreadCount++
        }
      }
    },

    /**
     * Xử lý chỉ báo Typing indicator
     */
    handleTypingIndicator(roomId, payload) {
      const authStore = useAuthStore()
      if (payload.senderUsername === authStore.user?.username) return

      const name = payload.senderFullName || payload.senderUsername
      if (!this.typingUsers[roomId]) {
        this.typingUsers[roomId] = []
      }

      if (!this.typingUsers[roomId].includes(name)) {
        this.typingUsers[roomId].push(name)
      }

      // Tự động tắt typing sau 2.5 giây nếu không nhận thêm tín hiệu
      setTimeout(() => {
        if (this.typingUsers[roomId]) {
          this.typingUsers[roomId] = this.typingUsers[roomId].filter(u => u !== name)
        }
      }, 2500)
    },

    /**
     * Gửi tin nhắn phòng chung toàn công ty
     */
    sendPublicMessage(content) {
      if (!this.client || !this.isConnected || !content?.trim()) return

      this.client.publish({
        destination: '/app/chat.sendPublic',
        body: JSON.stringify({
          roomId: 'GLOBAL',
          content: content.trim(),
          type: 'CHAT'
        })
      })
    },

    /**
     * Gửi tin nhắn riêng 1-1 tới một đồng nghiệp
     */
    sendPrivateMessage(recipientUsername, content) {
      if (!this.client || !this.isConnected || !content?.trim() || !recipientUsername) return

      this.client.publish({
        destination: '/app/chat.sendPrivate',
        body: JSON.stringify({
          recipientUsername: recipientUsername.trim(),
          content: content.trim(),
          type: 'CHAT'
        })
      })
    },

    /**
     * Gửi tín hiệu đang gõ phím
     */
    sendTyping() {
      if (!this.client || !this.isConnected) return

      if (this.activeChatType === 'PUBLIC') {
        this.client.publish({
          destination: '/app/chat.typing',
          body: JSON.stringify({
            roomId: 'GLOBAL'
          })
        })
      } else if (this.activeChatType === 'PRIVATE' && this.activePartner) {
        this.client.publish({
          destination: '/app/chat.typing',
          body: JSON.stringify({
            recipientUsername: this.activePartner.username
          })
        })
      }
    },

    /**
     * Chuyển đổi phòng chat
     */
    async selectRoom(type, partner = null) {
      this.activeChatType = type
      this.activePartner = partner

      if (type === 'PUBLIC') {
        this.activeRoomId = 'GLOBAL'
        await this.loadRoomHistory('GLOBAL')
        this.markAsRead('GLOBAL')
      } else if (partner) {
        const authStore = useAuthStore()
        this.activeRoomId = this.getPrivateRoomId(authStore.user?.username, partner.username)
        await this.loadPrivateHistory(partner.username)
        this.markAsRead(this.activeRoomId)
      }
    },

    /**
     * Tải lịch sử tin nhắn phòng chung
     */
    async loadRoomHistory(roomId = 'GLOBAL') {
      try {
        const res = await api.get(`/realtime-chat/history/${roomId}`)
        this.messagesByRoom[roomId] = res.data || []
      } catch (err) {
        console.error('Lỗi tải lịch sử phòng chat:', err)
      }
    },

    /**
     * Tải lịch sử tin nhắn riêng 1-1
     */
    async loadPrivateHistory(targetUsername) {
      try {
        const authStore = useAuthStore()
        const roomId = this.getPrivateRoomId(authStore.user?.username, targetUsername)
        const res = await api.get(`/realtime-chat/private-history/${targetUsername}`)
        this.messagesByRoom[roomId] = res.data || []
      } catch (err) {
        console.error('Lỗi tải lịch sử chat riêng:', err)
      }
    },

    /**
     * Đánh dấu đã đọc toàn bộ tin nhắn trong phòng
     */
    async markAsRead(roomId) {
      try {
        await api.put(`/realtime-chat/read/${roomId}`)
        if (this.unreadCounts[roomId]) {
          this.totalUnreadCount = Math.max(0, this.totalUnreadCount - this.unreadCounts[roomId])
          this.unreadCounts[roomId] = 0
        }
      } catch (err) {
        console.error('Lỗi mark as read:', err)
      }
    },

    /**
     * Lấy tổng số tin nhắn chưa đọc
     */
    async fetchTotalUnreadCount() {
      try {
        const res = await api.get('/realtime-chat/unread-count')
        this.totalUnreadCount = res.data?.unreadCount || 0
      } catch (err) {
        console.error('Lỗi lấy unread count:', err)
      }
    },

    /**
     * Lấy danh sách đồng nghiệp đang hoạt động
     */
    async fetchColleagues() {
      try {
        const res = await api.get('/realtime-chat/colleagues')
        this.colleagues = res.data || []
      } catch (err) {
        console.error('Lỗi lấy danh sách đồng nghiệp:', err)
      }
    },

    /**
     * Ngắt kết nối khi logout
     */
    disconnect() {
      if (this.client) {
        try {
          this.client.deactivate()
        } catch (e) {}
      }
      this.client = null
      this.isConnected = false
      this.isConnecting = false
      this.subscriptions = []
      this.messagesByRoom = {}
      this.unreadCounts = {}
      this.totalUnreadCount = 0
      this.typingUsers = {}
    },

    /**
     * Helper tạo ID phòng 1-1 chuẩn theo bảng chữ cái
     */
    getPrivateRoomId(u1, u2) {
      if (!u1 || !u2) return 'PRIVATE'
      return u1.localeCompare(u2) <= 0 ? `${u1}_${u2}` : `${u2}_${u1}`
    }
  }
})

