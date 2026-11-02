import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import api from '../api'
import { useAuthStore } from './auth'
import { 
  playNotificationSound, 
  showDesktopNotification, 
  requestNotificationPermission 
} from '../utils/soundNotification'

export const useRealtimeChatStore = defineStore('realtimeChat', {
  state: () => ({
    client: null,
    isConnected: false,
    isConnecting: false,

    // Cài đặt âm thanh và thông báo đẩy
    isSoundEnabled: localStorage.getItem('erp_chat_sound') !== 'false',
    isNotificationEnabled: typeof Notification !== 'undefined' && Notification.permission === 'granted',

    // Phòng chat hoặc đối tượng đang mở
    activeRoomId: 'GLOBAL', // 'GLOBAL' | 'user1_user2' | 'GROUP_123'
    activeChatType: 'PUBLIC', // 'PUBLIC' | 'PRIVATE' | 'GROUP'
    activePartner: null, // UserResponseDto khi chat 1-1
    activeGroup: null, // ChatGroupDto khi chat nhóm

    // Danh sách nhóm chat mà tôi tham gia
    myGroups: [],

    // Tin nhắn theo phòng: { [roomId]: [RealtimeMessageDto] }
    messagesByRoom: {},

    // Phân trang Lazy Loading theo phòng: { [roomId]: boolean }
    hasMoreHistory: {},
    isLoadingOlder: false,

    // Tin nhắn đang được Reply
    replyingTo: null,

    // Số tin chưa đọc theo phòng: { [roomId]: number }
    unreadCounts: {},
    totalUnreadCount: 0,

    // Danh sách đồng nghiệp để chat 1-1
    colleagues: [],

    // Biệt danh theo phòng: { [roomId]: { [username]: string } }
    roomNicknames: {},

    // Ai đang gõ phím: { [roomId]: Array<string> }
    typingUsers: {},

    // Subscriptions để quản lý hủy khi disconnect
    subscriptions: [],
    privateRoomSub: null,
    groupSubscriptions: {}
  }),

  getters: {
    currentMessages: (state) => state.messagesByRoom[state.activeRoomId] || [],
    currentTypingUsers: (state) => state.typingUsers[state.activeRoomId] || [],
    getNickname: (state) => (username, defaultName = '') => {
      if (!username) return defaultName || ''
      return state.roomNicknames[state.activeRoomId]?.[username] || defaultName || username
    }
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
        debug: () => {},
        onConnect: () => {
          this.isConnected = true
          this.isConnecting = false
          this.subscribeChannels()
          this.fetchTotalUnreadCount()
          this.fetchColleagues()
          this.fetchMyGroups()

          // Tải trước dữ liệu phòng hiện tại
          if (this.activeChatType === 'PUBLIC') {
            this.loadRoomHistory('GLOBAL')
          } else if (this.activeChatType === 'GROUP' && this.activeGroup) {
            this.loadRoomHistory(`GROUP_${this.activeGroup.id}`)
          } else if (this.activePartner) {
            this.loadPrivateHistory(this.activePartner.username)
          }
        },
        onDisconnect: () => {
          this.isConnected = false
          this.isConnecting = false
          this.subscriptions = []
          this.privateRoomSub = null
          this.groupSubscriptions = {}
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

      // 5. Nếu đang ở phòng riêng, subscribe thêm topic của phòng đó
      if (this.activeChatType === 'PRIVATE' && this.activePartner) {
        this.subscribeActivePrivateTopic()
      }

      // 6. Đăng ký các topic nhóm chat
      this.subscribeGroupTopics()
    },

    /**
     * Subscribe topic phòng riêng hiện tại
     */
    subscribeActivePrivateTopic() {
      if (!this.client || !this.isConnected || this.activeChatType !== 'PRIVATE' || !this.activeRoomId) return

      if (this.privateRoomSub) {
        try { this.privateRoomSub.unsubscribe() } catch (e) {}
        this.privateRoomSub = null
      }

      this.privateRoomSub = this.client.subscribe(`/topic/private.${this.activeRoomId}`, (message) => {
        try {
          const payload = JSON.parse(message.body)
          this.handleIncomingMessage(this.activeRoomId, payload)
        } catch (e) {
          console.error('Lỗi parse private topic message:', e)
        }
      })
    },

    /**
     * Đăng ký lắng nghe tin nhắn các Nhóm chat mà tôi tham gia
     */
    subscribeGroupTopics() {
      if (!this.client || !this.isConnected || !this.myGroups) return

      this.myGroups.forEach(g => {
        const roomId = `GROUP_${g.id}`
        if (!this.groupSubscriptions[roomId]) {
          const sub = this.client.subscribe(`/topic/room.${roomId}`, (message) => {
            try {
              const payload = JSON.parse(message.body)
              this.handleIncomingMessage(roomId, payload)
            } catch (e) {
              console.error('Lỗi parse group message:', e)
            }
          })
          this.groupSubscriptions[roomId] = sub
        }
        // Lắng nghe cả sự kiện typing trong nhóm
        if (!this.groupSubscriptions[`${roomId}_typing`]) {
          const typingSub = this.client.subscribe(`/topic/room.${roomId}.typing`, (message) => {
            try {
              const payload = JSON.parse(message.body)
              this.handleTypingIndicator(roomId, payload)
            } catch (e) {}
          })
          this.groupSubscriptions[`${roomId}_typing`] = typingSub
        }
      })
    },

    /**
     * Xử lý khi có tin nhắn mới hoặc sự kiện reaction tới
     */
    handleIncomingMessage(roomId, messageDto) {
      if (!roomId) return

      // --- TRƯỜNG HỢP 1: SỰ KIỆN REACTION ---
      if (messageDto.type === 'REACTION') {
        const targetRoom = messageDto.roomId || roomId
        if (this.messagesByRoom[targetRoom]) {
          const msg = this.messagesByRoom[targetRoom].find(m => m.id === messageDto.messageId)
          if (msg) {
            msg.reactions = messageDto.reactions || {}
          }
        }
        return
      }

      // --- TRƯỜNG HỢP 2: ĐÃ XEM TIN NHẮN (READ RECEIPT) ---
      if (messageDto.type === 'READ_RECEIPT') {
        const targetRoom = messageDto.roomId || roomId
        const authStore = useAuthStore()
        const myUsername = authStore.user?.username

        // Chỉ cập nhật trạng thái 'READ' cho tin nhắn do MÌNH gửi khi NGƯỜI KHÁC đọc
        if (messageDto.readerUsername && messageDto.readerUsername !== myUsername) {
          if (this.messagesByRoom[targetRoom]) {
            this.messagesByRoom[targetRoom].forEach(m => {
              if (m.senderUsername === myUsername) {
                m.status = 'READ'
              }
            })
          }
          const colleague = this.colleagues.find(c => 
            c.username === messageDto.readerUsername || 
            (myUsername && this.getPrivateRoomId(myUsername, c.username) === targetRoom)
          )
          if (colleague && colleague.lastMessageSender === myUsername) {
            colleague.lastMessageStatus = 'READ'
          }
          if (targetRoom && targetRoom.startsWith('GROUP_')) {
            const groupId = parseInt(targetRoom.replace('GROUP_', ''))
            const grp = this.myGroups.find(g => g.id === groupId)
            if (grp && grp.lastMessageSender === myUsername) {
              grp.lastMessageStatus = 'READ'
            }
          }
        }
        return
      }

      // --- TRƯỜNG HỢP 3: THU HỒI / XÓA TIN NHẮN ---
      if (messageDto.type === 'MESSAGE_DELETED') {
        const targetRoom = messageDto.roomId || roomId
        if (this.messagesByRoom[targetRoom]) {
          const msg = this.messagesByRoom[targetRoom].find(m => m.id === messageDto.messageId)
          if (msg) {
            msg.isDeleted = true
            msg.content = 'Tin nhắn đã được thu hồi'
          }
        }
        this.bumpConversationToTop(targetRoom, {
          content: 'Tin nhắn đã được thu hồi',
          isDeleted: true,
          createdAt: new Date().toISOString()
        })
        return
      }

      // --- TRƯỜNG HỢP 4: CHỈNH SỬA TIN NHẮN ---
      if (messageDto.type === 'MESSAGE_EDITED') {
        const targetRoom = messageDto.roomId || roomId
        if (this.messagesByRoom[targetRoom]) {
          const msg = this.messagesByRoom[targetRoom].find(m => m.id === messageDto.messageId)
          if (msg) {
            msg.content = messageDto.newContent
            msg.isEdited = true
            msg.editedAt = messageDto.editedAt
          }
        }
        return
      }

      // --- TRƯỜNG HỢP 5: CẬP NHẬT BIỆT DANH (NICKNAME) ---
      if (messageDto.type === 'NICKNAME_UPDATED') {
        const targetRoom = messageDto.roomId || roomId
        if (!this.roomNicknames[targetRoom]) {
          this.roomNicknames[targetRoom] = {}
        }
        this.roomNicknames[targetRoom][messageDto.username] = messageDto.nickname
        return
      }

      // --- TRƯỜNG HỢP 6: TIN NHẮN MỚI ---
      let targetRoom = roomId
      if (targetRoom && targetRoom.includes('_') && !targetRoom.startsWith('GROUP_')) {
        targetRoom = targetRoom.toLowerCase()
      }

      if (!this.messagesByRoom[targetRoom]) {
        this.messagesByRoom[targetRoom] = []
      }

      const authStore = useAuthStore()
      const myUsername = authStore.user?.username
      const isMyMessage = messageDto.senderUsername?.toLowerCase() === myUsername?.toLowerCase()

      // Kiểm tra xem có tin nhắn tạm thời (optimistic update) của chính mình đang chờ server phản hồi không
      const candidateRooms = Array.from(new Set([targetRoom, this.activeRoomId, roomId].filter(Boolean)))
      let replaced = false
      for (const r of candidateRooms) {
        if (this.messagesByRoom[r]) {
          const tempIndex = this.messagesByRoom[r].findIndex(m => 
            typeof m.id === 'string' && m.id.startsWith('temp_') &&
            m.senderUsername?.toLowerCase() === messageDto.senderUsername?.toLowerCase() &&
            m.content === messageDto.content
          )
          if (tempIndex !== -1) {
            this.messagesByRoom[r][tempIndex] = { ...messageDto, status: messageDto.status || 'SENT' }
            replaced = true
            break
          }
        }
      }

      if (!replaced) {
        // Tránh trùng lặp tin nhắn theo ID
        const exists = this.messagesByRoom[targetRoom].some(m => m.id === messageDto.id)
        if (!exists) {
          this.messagesByRoom[targetRoom].push(messageDto)
        }
      }

      // Nếu đang mở đúng phòng này
      if (this.activeRoomId === roomId) {
        if (!isMyMessage) {
          if (typeof document !== 'undefined' && !document.hidden) {
            this.markAsRead(roomId)
          } else {
            this.unreadCounts[roomId] = (this.unreadCounts[roomId] || 0) + 1
            this.totalUnreadCount++
          }
        }
      } else {
        // Tăng badge số tin chưa đọc cho phòng này
        if (!isMyMessage) {
          this.unreadCounts[roomId] = (this.unreadCounts[roomId] || 0) + 1
          this.totalUnreadCount++
        }
      }

      // Phát âm thanh thông báo và Desktop Notification nếu là tin nhắn đến từ người khác
      if (!isMyMessage) {
        if (this.isSoundEnabled) {
          playNotificationSound()
        }
        if (document.hidden) {
          const senderName = messageDto.senderFullName || messageDto.senderUsername || 'Đồng nghiệp'
          const previewText = messageDto.type === 'IMAGE' ? '📷 [Đã gửi một hình ảnh]' : messageDto.content
          showDesktopNotification(`Tin nhắn mới từ ${senderName}`, {
            body: previewText,
            icon: messageDto.senderAvatarUrl
          })
        }
      }

      // Tự động đẩy cuộc trò chuyện lên đầu danh sách và cập nhật tin nhắn xem trước
      this.bumpConversationToTop(roomId, messageDto)
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
     * Gán tin nhắn đang được Reply
     */
    setReplyingTo(message) {
      this.replyingTo = message
    },

    /**
     * Hủy bỏ Reply
     */
    cancelReply() {
      this.replyingTo = null
    },

    /**
     * Gửi tin nhắn phòng chung hoặc phòng nhóm (với Optimistic UI)
     */
    sendPublicMessage(content) {
      if (!content?.trim() || !this.isConnected) return
      const authStore = useAuthStore()
      const targetRoomId = this.activeRoomId || 'GLOBAL'

      const replyToId = this.replyingTo?.id || null
      const replyToSender = this.replyingTo ? (this.replyingTo.senderFullName || this.replyingTo.senderUsername) : null
      const replyToContent = this.replyingTo ? this.replyingTo.content : null

      // Optimistic Update: Thêm ngay vào UI phòng với ID tạm
      const tempId = 'temp_' + Date.now()
      const optimisticMsg = {
        id: tempId,
        roomId: targetRoomId,
        senderUsername: authStore.user?.username,
        senderFullName: authStore.user?.fullName || authStore.user?.username,
        senderAvatarUrl: authStore.user?.avatarUrl,
        recipientUsername: null,
        content: content.trim(),
        type: 'CHAT',
        status: 'SENDING',
        createdAt: new Date().toISOString(),
        replyToId,
        replyToSender,
        replyToContent,
        reactions: {}
      }

      if (!this.messagesByRoom[targetRoomId]) this.messagesByRoom[targetRoomId] = []
      this.messagesByRoom[targetRoomId].push(optimisticMsg)
      this.replyingTo = null

      if (targetRoomId.startsWith('GROUP_')) {
        this.bumpConversationToTop(targetRoomId, optimisticMsg)
      }

      this.client.publish({
        destination: '/app/chat.sendPublic',
        body: JSON.stringify({
          roomId: targetRoomId,
          senderUsername: authStore.user?.username,
          content: content.trim(),
          type: 'CHAT',
          replyToId,
          replyToSender,
          replyToContent
        })
      })
    },

    /**
     * Gửi tin nhắn riêng 1-1 tới một đồng nghiệp (với Optimistic UI hiển thị tức thì)
     */
    sendPrivateMessage(recipientUsername, content) {
      if (!content?.trim() || !recipientUsername || !this.isConnected) return
      const authStore = useAuthStore()
      const myUsername = authStore.user?.username
      const roomId = this.getPrivateRoomId(myUsername, recipientUsername)

      const replyToId = this.replyingTo?.id || null
      const replyToSender = this.replyingTo ? (this.replyingTo.senderFullName || this.replyingTo.senderUsername) : null
      const replyToContent = this.replyingTo ? this.replyingTo.content : null

      // Optimistic Update: Thêm ngay vào UI phòng riêng để người gửi nhìn thấy lập tức
      const tempId = 'temp_' + Date.now()
      const optimisticMsg = {
        id: tempId,
        roomId,
        senderUsername: myUsername,
        senderFullName: authStore.user?.fullName || myUsername,
        senderAvatarUrl: authStore.user?.avatarUrl,
        recipientUsername: recipientUsername.trim(),
        content: content.trim(),
        type: 'CHAT',
        status: 'SENDING',
        createdAt: new Date().toISOString(),
        replyToId,
        replyToSender,
        replyToContent,
        reactions: {}
      }

      if (!this.messagesByRoom[roomId]) this.messagesByRoom[roomId] = []
      this.messagesByRoom[roomId].push(optimisticMsg)
      this.replyingTo = null

      this.bumpConversationToTop(roomId, optimisticMsg)

      this.client.publish({
        destination: '/app/chat.sendPrivate',
        body: JSON.stringify({
          recipientUsername: recipientUsername.trim(),
          senderUsername: myUsername,
          content: content.trim(),
          type: 'CHAT',
          replyToId,
          replyToSender,
          replyToContent
        })
      })
    },

    /**
     * Gửi hình ảnh vào phòng chat chung hoặc phòng nhóm
     */
    sendPublicImage(imageUrl, roomId = null) {
      if (!this.client || !this.isConnected || !imageUrl) return
      const authStore = useAuthStore()
      const targetRoomId = roomId || this.activeRoomId || 'GLOBAL'

      const tempId = 'temp_' + Date.now()
      const optimisticMsg = {
        id: tempId,
        roomId: targetRoomId,
        senderUsername: authStore.user?.username,
        senderFullName: authStore.user?.fullName || authStore.user?.username,
        senderAvatarUrl: authStore.user?.avatarUrl,
        recipientUsername: null,
        content: imageUrl,
        type: 'IMAGE',
        status: 'SENDING',
        createdAt: new Date().toISOString(),
        reactions: {}
      }

      if (!this.messagesByRoom[targetRoomId]) this.messagesByRoom[targetRoomId] = []
      this.messagesByRoom[targetRoomId].push(optimisticMsg)
      this.replyingTo = null

      if (targetRoomId.startsWith('GROUP_')) {
        this.bumpConversationToTop(targetRoomId, optimisticMsg)
      }

      this.client.publish({
        destination: '/app/chat.sendPublic',
        body: JSON.stringify({
          roomId: targetRoomId,
          senderUsername: authStore.user?.username,
          content: imageUrl,
          type: 'IMAGE'
        })
      })
    },

    /**
     * Gửi hình ảnh trong chat riêng 1-1
     */
    sendPrivateImage(recipientUsername, imageUrl) {
      if (!this.client || !this.isConnected || !imageUrl || !recipientUsername) return
      const authStore = useAuthStore()
      const myUsername = authStore.user?.username
      const roomId = this.getPrivateRoomId(myUsername, recipientUsername)

      const tempId = 'temp_' + Date.now()
      const optimisticMsg = {
        id: tempId,
        roomId,
        senderUsername: myUsername,
        senderFullName: authStore.user?.fullName || myUsername,
        senderAvatarUrl: authStore.user?.avatarUrl,
        recipientUsername: recipientUsername.trim(),
        content: imageUrl,
        type: 'IMAGE',
        status: 'SENDING',
        createdAt: new Date().toISOString(),
        reactions: {}
      }

      if (!this.messagesByRoom[roomId]) this.messagesByRoom[roomId] = []
      this.messagesByRoom[roomId].push(optimisticMsg)
      this.replyingTo = null

      this.bumpConversationToTop(roomId, optimisticMsg)

      this.client.publish({
        destination: '/app/chat.sendPrivate',
        body: JSON.stringify({
          recipientUsername: recipientUsername.trim(),
          senderUsername: myUsername,
          content: imageUrl,
          type: 'IMAGE'
        })
      })
    },

    /**
     * Thả hoặc hủy cảm xúc (Reaction) trên tin nhắn
     */
    async toggleReaction(messageId, emoji) {
      const authStore = useAuthStore()
      const username = authStore.user?.username
      if (!username || !messageId || !emoji) return

      // Optimistic update giao diện reaction
      const currentList = this.messagesByRoom[this.activeRoomId] || []
      const msg = currentList.find(m => m.id === messageId)
      if (msg) {
        if (!msg.reactions) msg.reactions = {}
        const userList = msg.reactions[emoji] || []
        if (userList.includes(username)) {
          msg.reactions[emoji] = userList.filter(u => u !== username)
          if (msg.reactions[emoji].length === 0) {
            delete msg.reactions[emoji]
          }
        } else {
          msg.reactions[emoji] = [...userList, username]
        }
      }

      // Gửi qua WebSocket STOMP
      if (this.client && this.isConnected) {
        this.client.publish({
          destination: '/app/chat.react',
          body: JSON.stringify({
            messageId,
            emoji,
            username,
            roomId: this.activeRoomId
          })
        })
      } else {
        try {
          await api.post('/realtime-chat/react', { messageId, emoji })
        } catch (e) {
          console.error('Lỗi toggle reaction fallback:', e)
        }
      }
    },

    /**
     * Bật / Tắt âm thanh thông báo
     */
    toggleSound() {
      this.isSoundEnabled = !this.isSoundEnabled
      localStorage.setItem('erp_chat_sound', String(this.isSoundEnabled))
    },

    /**
     * Kích hoạt quyền hiển thị thông báo Desktop trên màn hình
     */
    async enableDesktopNotification() {
      const granted = await requestNotificationPermission()
      this.isNotificationEnabled = granted
      return granted
    },

    /**
     * Gửi tín hiệu đang gõ phím
     */
    sendTyping() {
      if (!this.client || !this.isConnected) return
      const authStore = useAuthStore()

      if (this.activeChatType === 'PUBLIC' || this.activeChatType === 'GROUP') {
        this.client.publish({
          destination: '/app/chat.typing',
          body: JSON.stringify({
            roomId: this.activeRoomId,
            senderUsername: authStore.user?.username
          })
        })
      } else if (this.activeChatType === 'PRIVATE' && this.activePartner) {
        this.client.publish({
          destination: '/app/chat.typing',
          body: JSON.stringify({
            recipientUsername: this.activePartner.username,
            senderUsername: authStore.user?.username
          })
        })
      }
    },

    /**
     * Chuyển đổi phòng chat
     */
    async selectRoom(type, target = null) {
      this.activeChatType = type
      this.replyingTo = null

      if (type === 'PUBLIC') {
        this.activeRoomId = 'GLOBAL'
        this.activePartner = null
        this.activeGroup = null
        if (this.privateRoomSub) {
          try { this.privateRoomSub.unsubscribe() } catch (e) {}
          this.privateRoomSub = null
        }
        await this.loadRoomHistory('GLOBAL')
        this.markAsRead('GLOBAL')
        this.fetchNicknames('GLOBAL')
      } else if (type === 'PRIVATE' && target) {
        this.activePartner = target
        this.activeGroup = null
        const authStore = useAuthStore()
        this.activeRoomId = this.getPrivateRoomId(authStore.user?.username, target.username)
        this.subscribeActivePrivateTopic()
        await this.loadPrivateHistory(target.username)
        this.markAsRead(this.activeRoomId)
        this.fetchNicknames(this.activeRoomId)
      } else if (type === 'GROUP' && target) {
        this.activeGroup = target
        this.activePartner = null
        this.activeRoomId = `GROUP_${target.id}`
        if (this.privateRoomSub) {
          try { this.privateRoomSub.unsubscribe() } catch (e) {}
          this.privateRoomSub = null
        }
        this.subscribeGroupTopics()
        await this.loadRoomHistory(this.activeRoomId)
        this.markAsRead(this.activeRoomId)
        this.fetchNicknames(this.activeRoomId)
      }
    },

    /**
     * Tải lịch sử ban đầu phòng chat chung hoặc nhóm
     */
    async loadRoomHistory(roomId = 'GLOBAL') {
      try {
        const res = await api.get(`/realtime-chat/history/${roomId}`)
        this.messagesByRoom[roomId] = res.data || []
        this.hasMoreHistory[roomId] = (res.data?.length || 0) >= 20
      } catch (err) {
        console.error('Lỗi tải lịch sử phòng chat:', err)
      }
    },

    /**
     * Tải lịch sử ban đầu chat riêng 1-1
     */
    async loadPrivateHistory(targetUsername) {
      try {
        const authStore = useAuthStore()
        const roomId = this.getPrivateRoomId(authStore.user?.username, targetUsername)
        const res = await api.get(`/realtime-chat/private-history/${targetUsername}`)
        this.messagesByRoom[roomId] = res.data || []
        this.hasMoreHistory[roomId] = (res.data?.length || 0) >= 20
      } catch (err) {
        console.error('Lỗi tải lịch sử chat riêng:', err)
      }
    },

    /**
     * Lazy Loading tin nhắn cũ hơn khi cuộn lên đỉnh
     */
    async loadOlderMessages(roomId) {
      if (this.isLoadingOlder || this.hasMoreHistory[roomId] === false) return

      const currentList = this.messagesByRoom[roomId] || []
      if (currentList.length === 0) return

      // Tìm tin nhắn có ID thật nhỏ nhất (cũ nhất)
      const oldestValid = currentList.find(m => typeof m.id === 'number')
      if (!oldestValid) return

      this.isLoadingOlder = true
      try {
        let res
        if (roomId === 'GLOBAL' || roomId.startsWith('GROUP_')) {
          res = await api.get(`/realtime-chat/history/${roomId}/page`, {
            params: { beforeId: oldestValid.id, limit: 20 }
          })
        } else if (this.activePartner) {
          res = await api.get(`/realtime-chat/private-history/${this.activePartner.username}/page`, {
            params: { beforeId: oldestValid.id, limit: 20 }
          })
        }

        const olderMsgs = res?.data || []
        if (olderMsgs.length < 20) {
          this.hasMoreHistory[roomId] = false
        }
        if (olderMsgs.length > 0) {
          const existingIds = new Set(currentList.map(m => m.id))
          const filteredNew = olderMsgs.filter(m => !existingIds.has(m.id))
          this.messagesByRoom[roomId] = [...filteredNew, ...currentList]
        }
      } catch (err) {
        console.error('Lỗi tải tin nhắn cũ:', err)
      } finally {
        this.isLoadingOlder = false
      }
    },

    /**
     * Đánh dấu đã đọc toàn bộ tin nhắn trong phòng
     */
    async markAsRead(roomId) {
      if (!roomId) return
      const authStore = useAuthStore()

      // Gửi tín hiệu STOMP thời gian thực để bên kia nhận biết ngay
      if (this.client && this.isConnected) {
        this.client.publish({
          destination: '/app/chat.markRead',
          body: JSON.stringify({
            roomId,
            readerUsername: authStore.user?.username
          })
        })
      }

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
     * Lấy danh sách đồng nghiệp
     */
    async fetchColleagues() {
      try {
        const res = await api.get('/realtime-chat/colleagues')
        this.colleagues = res.data || []
      } catch (err) {
        console.error('Lỗi tải danh bạ đồng nghiệp:', err)
      }
    },

    /**
     * Lấy danh sách nhóm chat của tôi
     */
    async fetchMyGroups() {
      try {
        const res = await api.get('/realtime-chat/groups/my-groups')
        this.myGroups = res.data || []
        this.subscribeGroupTopics()
      } catch (err) {
        console.error('Lỗi tải danh sách nhóm:', err)
      }
    },

    /**
     * Tạo nhóm chat mới
     */
    async createGroup(name, memberUsernames) {
      const res = await api.post('/realtime-chat/groups', { name, memberUsernames })
      const newGroup = res.data
      this.myGroups.unshift(newGroup)
      this.subscribeGroupTopics()
      await this.selectRoom('GROUP', newGroup)
      return newGroup
    },

    /**
     * Thêm thành viên vào nhóm chat
     */
    async addMembersToGroup(groupId, usernames) {
      const res = await api.post(`/realtime-chat/groups/${groupId}/members`, { usernames })
      const updated = res.data
      this.activeGroup = updated
      const idx = this.myGroups.findIndex(g => g.id === groupId)
      if (idx !== -1) {
        this.myGroups[idx] = updated
      }
      return updated
    },

    /**
     * Xóa thành viên khỏi nhóm chat
     */
    async removeMemberFromGroup(groupId, targetUsername) {
      await api.delete(`/realtime-chat/groups/${groupId}/members/${targetUsername}`)
      if (this.activeGroup && this.activeGroup.id === groupId) {
        this.activeGroup.members = this.activeGroup.members.filter(m => m.username !== targetUsername)
        this.activeGroup.memberCount = this.activeGroup.members.length
        const idx = this.myGroups.findIndex(g => g.id === groupId)
        if (idx !== -1) {
          this.myGroups[idx] = { ...this.activeGroup }
        }
      }
    },

    /**
     * Rời khỏi nhóm chat
     */
    async leaveGroup(groupId) {
      await api.post(`/realtime-chat/groups/${groupId}/leave`)
      const roomId = `GROUP_${groupId}`
      if (this.groupSubscriptions[roomId]) {
        try { this.groupSubscriptions[roomId].unsubscribe() } catch (e) {}
        delete this.groupSubscriptions[roomId]
      }
      this.myGroups = this.myGroups.filter(g => g.id !== groupId)
      if (this.activeGroup?.id === groupId) {
        await this.selectRoom('PUBLIC')
      }
    },

    /**
     * Tự động đưa cuộc trò chuyện lên đầu danh bạ khi có tin nhắn mới
     */
    bumpConversationToTop(roomId, messageDto) {
      if (!roomId || !messageDto || messageDto.type === 'REACTION') return
      const authStore = useAuthStore()
      const myUsername = authStore.user?.username

      let preview = messageDto.content
      if (messageDto.isDeleted) {
        preview = 'Tin nhắn đã được thu hồi'
      } else if (messageDto.type === 'IMAGE') {
        preview = '📷 [Hình ảnh]'
      } else if (messageDto.type === 'VIDEO') {
        preview = '🎥 [Video]'
      }

      const time = messageDto.createdAt || new Date().toISOString()
      const status = messageDto.status || 'SENT'
      const sender = messageDto.senderUsername

      // 1. Nhóm chat: roomId dạng GROUP_{groupId}
      if (roomId.startsWith('GROUP_')) {
        const groupId = roomId.replace('GROUP_', '')
        const idx = this.myGroups.findIndex(g => String(g.id) === String(groupId))
        if (idx !== -1) {
          const group = { 
            ...this.myGroups[idx], 
            lastMessageContent: preview, 
            lastMessageTime: time,
            lastMessageStatus: status,
            lastMessageSender: sender
          }
          this.myGroups.splice(idx, 1)
          this.myGroups.unshift(group)
        }
      } 
      // 2. Chat riêng 1-1 (không phải GLOBAL)
      else if (roomId !== 'GLOBAL') {
        let partnerUsername = null
        if (messageDto.senderUsername && messageDto.recipientUsername) {
          partnerUsername = messageDto.senderUsername === myUsername 
            ? messageDto.recipientUsername 
            : messageDto.senderUsername
        } else if (roomId.includes('_')) {
          const parts = roomId.split('_')
          partnerUsername = parts[0] === myUsername ? parts[1] : parts[0]
        }

        if (partnerUsername) {
          const idx = this.colleagues.findIndex(c => c.username === partnerUsername)
          if (idx !== -1) {
            const colleague = { 
              ...this.colleagues[idx], 
              lastMessageContent: preview, 
              lastMessageTime: time,
              lastMessageStatus: status,
              lastMessageSender: sender
            }
            this.colleagues.splice(idx, 1)
            this.colleagues.unshift(colleague)
          }
        }
      }
    },

    /**
     * Thu hồi / Xóa tin nhắn
     */
    async deleteMessage(messageId) {
      if (!messageId) return
      const authStore = useAuthStore()
      const username = authStore.user?.username

      // Optimistic update
      const msgList = this.messagesByRoom[this.activeRoomId] || []
      const msg = msgList.find(m => m.id === messageId)
      if (msg) {
        msg.isDeleted = true
        msg.content = 'Tin nhắn đã được thu hồi'
      }

      if (this.client && this.isConnected) {
        this.client.publish({
          destination: '/app/chat.deleteMessage',
          body: JSON.stringify({ messageId, username })
        })
      } else {
        await api.delete(`/realtime-chat/messages/${messageId}`)
      }
    },

    /**
     * Chỉnh sửa nội dung tin nhắn
     */
    async editMessage(messageId, newContent) {
      if (!messageId || !newContent?.trim()) return
      const authStore = useAuthStore()
      const username = authStore.user?.username
      const clean = newContent.trim()

      // Optimistic update
      const msgList = this.messagesByRoom[this.activeRoomId] || []
      const msg = msgList.find(m => m.id === messageId)
      if (msg) {
        msg.content = clean
        msg.isEdited = true
        msg.editedAt = new Date().toISOString()
      }

      if (this.client && this.isConnected) {
        this.client.publish({
          destination: '/app/chat.editMessage',
          body: JSON.stringify({ messageId, username, newContent: clean })
        })
      } else {
        await api.put(`/realtime-chat/messages/${messageId}`, { content: clean })
      }
    },

    /**
     * Gửi Video phòng chung hoặc nhóm
     */
    sendPublicVideo(videoUrl, roomId = null) {
      if (!this.client || !this.isConnected || !videoUrl) return
      const authStore = useAuthStore()
      const targetRoomId = roomId || this.activeRoomId || 'GLOBAL'

      const tempId = 'temp_' + Date.now()
      const optimisticMsg = {
        id: tempId,
        roomId: targetRoomId,
        senderUsername: authStore.user?.username,
        senderFullName: authStore.user?.fullName || authStore.user?.username,
        senderAvatarUrl: authStore.user?.avatarUrl,
        recipientUsername: null,
        content: videoUrl,
        type: 'VIDEO',
        status: 'SENDING',
        createdAt: new Date().toISOString(),
        reactions: {}
      }

      if (!this.messagesByRoom[targetRoomId]) this.messagesByRoom[targetRoomId] = []
      this.messagesByRoom[targetRoomId].push(optimisticMsg)
      this.replyingTo = null

      if (targetRoomId.startsWith('GROUP_')) {
        this.bumpConversationToTop(targetRoomId, optimisticMsg)
      }

      this.client.publish({
        destination: '/app/chat.sendPublic',
        body: JSON.stringify({
          roomId: targetRoomId,
          senderUsername: authStore.user?.username,
          content: videoUrl,
          type: 'VIDEO'
        })
      })
    },

    /**
     * Gửi Video chat riêng 1-1
     */
    sendPrivateVideo(recipientUsername, videoUrl) {
      if (!this.client || !this.isConnected || !videoUrl || !recipientUsername) return
      const authStore = useAuthStore()
      const myUsername = authStore.user?.username
      const roomId = this.getPrivateRoomId(myUsername, recipientUsername)

      const tempId = 'temp_' + Date.now()
      const optimisticMsg = {
        id: tempId,
        roomId,
        senderUsername: myUsername,
        senderFullName: authStore.user?.fullName || myUsername,
        senderAvatarUrl: authStore.user?.avatarUrl,
        recipientUsername: recipientUsername.trim(),
        content: videoUrl,
        type: 'VIDEO',
        status: 'SENDING',
        createdAt: new Date().toISOString(),
        reactions: {}
      }

      if (!this.messagesByRoom[roomId]) this.messagesByRoom[roomId] = []
      this.messagesByRoom[roomId].push(optimisticMsg)
      this.replyingTo = null

      this.bumpConversationToTop(roomId, optimisticMsg)

      this.client.publish({
        destination: '/app/chat.sendPrivate',
        body: JSON.stringify({
          recipientUsername: recipientUsername.trim(),
          senderUsername: myUsername,
          content: videoUrl,
          type: 'VIDEO'
        })
      })
    },

    /**
     * Tải danh sách biệt danh của phòng
     */
    async fetchNicknames(roomId) {
      if (!roomId) return
      try {
        const res = await api.get(`/realtime-chat/rooms/${roomId}/nicknames`)
        if (!this.roomNicknames[roomId]) {
          this.roomNicknames[roomId] = {}
        }
        this.roomNicknames[roomId] = { ...this.roomNicknames[roomId], ...res.data }
      } catch (err) {
        console.error('Lỗi tải biệt danh:', err)
      }
    },

    /**
     * Đặt biệt danh cho thành viên trong phòng
     */
    async setNickname(roomId, targetUsername, nickname) {
      if (!roomId || !targetUsername) return
      try {
        const res = await api.post(`/realtime-chat/rooms/${roomId}/nicknames`, {
          username: targetUsername,
          nickname
        })
        if (!this.roomNicknames[roomId]) {
          this.roomNicknames[roomId] = {}
        }
        this.roomNicknames[roomId][targetUsername] = nickname
        return res.data
      } catch (err) {
        console.error('Lỗi đặt biệt danh:', err)
        throw err
      }
    },

    /**
     * Sinh Room ID 1-1 cố định theo thứ tự bảng chữ cái (luôn chuẩn hóa chữ thường để đồng bộ 100% với Backend)
     */
    getPrivateRoomId(u1, u2) {
      if (!u1 || !u2) return 'PRIVATE'
      const s1 = u1.trim().toLowerCase()
      const s2 = u2.trim().toLowerCase()
      return s1 <= s2 ? `${s1}_${s2}` : `${s2}_${s1}`
    }
  }
})
