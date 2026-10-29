<template>
  <el-container class="admin-layout">
    <!-- DESKTOP SIDEBAR (Visible on >= 992px) - FIGMA IMAGES 3 & 4 -->
    <el-aside 
      v-if="!isMobile" 
      :width="isCollapse ? '72px' : '248px'" 
      class="desktop-sidebar"
      :class="{ 'is-collapsed': isCollapse }"
    >
      <!-- Sidebar Brand Top -->
      <div class="sidebar-brand-header">
        <div class="brand-left" @click="$router.push('/')">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 15a4 4 0 004 4h11a3 3 0 000-6 5.5 5.5 0 00-10.7-1.8A4.002 4.002 0 003 15z" fill="#2563eb"/>
              <path stroke-linecap="round" stroke-linejoin="round" d="M8.5 14.5l2 2 4.5-4.5" stroke="#ffffff" stroke-width="2.2" />
            </svg>
          </div>
          <div v-if="!isCollapse" class="brand-text-block">
            <div class="brand-name">ERP Cloud</div>
            <span class="enterprise-tag">ENTERPRISE SUITE</span>
          </div>
        </div>

        <button 
          type="button" 
          class="btn-collapse-sidebar" 
          :title="isCollapse ? 'Mở rộng thanh menu' : 'Thu gọn thanh menu'"
          @click="toggleSidebar"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <line x1="15" y1="3" x2="15" y2="21"/>
          </svg>
        </button>
      </div>

      <!-- Navigation Section Label -->
      <div v-if="!isCollapse" class="nav-section-title">
        ĐIỀU HƯỚNG CHÍNH
      </div>

      <!-- Main Navigation Menu -->
      <el-menu 
        :default-active="$route.path" 
        router 
        :collapse="isCollapse"
        background-color="transparent" 
        class="custom-enterprise-menu"
      >
        <el-menu-item index="/">
          <el-icon><Monitor /></el-icon>
          <template #title>
            <span class="menu-title-text">Tổng quan Dashboard</span>
          </template>
        </el-menu-item>

        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <template #title>
            <span class="menu-title-text">Quản lý Sản phẩm</span>
          </template>
        </el-menu-item>

        <el-menu-item index="/orders" @click.prevent="notifyFeature('Quản lý Đơn hàng')">
          <el-icon><Tickets /></el-icon>
          <template #title>
            <span class="menu-title-text">Quản lý Đơn hàng</span>
          </template>
        </el-menu-item>

        <el-menu-item v-if="authStore.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <template #title>
            <span class="menu-title-text">Quản lý Tài khoản</span>
          </template>
        </el-menu-item>

        <el-menu-item index="/ai-copilot" @click.prevent="openChatWidget">
          <el-icon class="ai-sparkle-icon"><Cpu /></el-icon>
          <template #title>
            <div class="ai-menu-item-row">
              <span class="menu-title-text">Trợ lý AI Copilot</span>
              <span class="ai-pill-tag">LIVE</span>
            </div>
          </template>
        </el-menu-item>

        <el-menu-item v-if="authStore.isAdmin" index="/announcements">
          <el-icon><BellFilled /></el-icon>
          <template #title>
            <span class="menu-title-text">Quản lý Thông báo</span>
          </template>
        </el-menu-item>

        <el-menu-item index="/analytics" @click.prevent="notifyFeature('Báo cáo & Phân tích')">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>
            <span class="menu-title-text">Báo cáo & Phân tích</span>
          </template>
        </el-menu-item>

        <el-menu-item index="/settings" @click.prevent="profileDialogVisible = true">
          <el-icon><Setting /></el-icon>
          <template #title>
            <span class="menu-title-text">Cài đặt hệ thống</span>
          </template>
        </el-menu-item>
      </el-menu>

      <!-- Bottom Info Card (Figma Match) -->
      <div class="sidebar-bottom-card" v-if="!isCollapse">
        <div class="server-status-row">
          <span class="pulse-green-dot"></span>
          <span class="server-title">Hệ thống hoạt động</span>
        </div>
        <div class="server-version">v4.2.0 • Máy chủ Việt Nam</div>
      </div>
      <div v-else class="sidebar-bottom-icon" title="Hệ thống hoạt động: v4.2.0">
        <span class="pulse-green-dot"></span>
      </div>
    </el-aside>

    <!-- MOBILE DRAWER SIDEBAR (<992px) -->
    <el-drawer
      v-else
      v-model="mobileDrawerVisible"
      direction="ltr"
      size="260px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-header">
        <div class="brand-left">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.2">
              <circle cx="12" cy="12" r="3" fill="currentColor" />
              <path stroke-linecap="round" d="M12 3v3m0 12v3M3 12h3m12 0h3M5.6 5.6l2.1 2.1m8.6 8.6l2.1 2.1M5.6 18.4l2.1-2.1m8.6-8.6l2.1-2.1" />
            </svg>
          </div>
          <div class="brand-text-block">
            <span class="brand-name">ERP Cloud</span>
            <span class="enterprise-tag">ENTERPRISE SUITE</span>
          </div>
        </div>
      </div>

      <el-menu 
        :default-active="$route.path" 
        router 
        class="custom-enterprise-menu"
        @select="mobileDrawerVisible = false"
      >
        <el-menu-item index="/">
          <el-icon><Monitor /></el-icon>
          <template #title>Dashboard</template>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <template #title>Quản lý Sản phẩm</template>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <template #title>Quản lý Tài khoản</template>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/announcements">
          <el-icon><BellFilled /></el-icon>
          <template #title>Quản lý Thông báo</template>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <!-- MAIN APP CONTENT WRAPPER -->
    <el-container class="main-wrapper">
      <!-- ENTERPRISE TOP HEADER (FIGMA EXACT MATCH) -->
      <el-header class="top-header">
        <!-- Left: Inline Breadcrumb (ERP Core › Không gian làm việc) -->
        <div class="header-left-group">
          <button 
            v-if="isMobile" 
            type="button" 
            class="btn-mobile-toggle" 
            @click="mobileDrawerVisible = true"
          >
            <el-icon :size="20"><Expand /></el-icon>
          </button>

          <div class="header-breadcrumb-inline">
            <span class="bc-root">ERP Core</span>
            <span class="bc-arrow">›</span>
            <span class="bc-leaf">Không gian làm việc</span>
          </div>
        </div>

        <!-- Center: Real-time Live Clock Card (Múi giờ GMT+7) -->
        <div class="header-center-clock" v-if="!isMobile">
          <div 
            class="clean-clock-card" 
            :class="{ 
              'is-day': isDaytime, 
              'is-night': !isDaytime,
              'is-top-hour': isTopOfTheHour 
            }"
          >
            <!-- Day / Night Icon: Mặt trời ban ngày / Mặt trăng ban đêm -->
            <div class="day-night-icon" :title="isDaytime ? 'Ban ngày (6:00 - 18:00)' : 'Ban đêm (18:00 - 6:00)'">
              <el-icon v-if="isDaytime" class="sun-icon"><Sunny /></el-icon>
              <el-icon v-else class="moon-icon"><Moon /></el-icon>
            </div>

            <div class="clock-content-wrap">
              <div class="clock-time-line">
                <span class="clock-digits" :class="{ 'scale-digits': isTopOfTheHour }">
                  {{ clockHours }}:{{ clockMinutes }}
                </span>
                <span class="clock-seconds">:{{ clockSeconds }}</span>
                <span class="clock-ampm">{{ clockPeriod }}</span>
              </div>
              <div class="clock-date-line">
                <span class="clock-day">{{ clockDayName }},</span>
                <span class="clock-calendar">{{ clockDateFormatted }}</span>
                <span class="clock-zone-pill">GMT+7</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Actions, Bell, Help, User Profile with Online Status on Avatar -->
        <div class="header-right-group">
          <!-- Notification Bell with Red Dot -->
          <div class="header-icon-wrap" @click="$router.push('/announcements')">
            <button type="button" class="btn-header-icon" title="Thông báo hệ thống">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h24s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
              <span class="bell-red-badge"></span>
            </button>
          </div>

          <!-- Help Center Icon [?] -->
          <div class="header-icon-wrap" @click="notifyFeature('Trung tâm trợ giúp')">
            <button type="button" class="btn-header-icon" title="Trung tâm trợ giúp">
              <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </button>
          </div>

          <!-- User Profile Dropdown: Name & Online Status on Left, Dark Avatar with Online Dot on Right -->
          <el-dropdown trigger="click" @command="handleUserMenuCommand" placement="bottom-end">
            <div class="user-profile-trigger">
              <div class="user-meta-block" v-if="!isMobile">
                <div class="user-name">@{{ authStore.user?.username || 'admin' }}</div>
                <div class="user-status-online-pill">
                  <span class="online-pulse-dot"></span>
                  <span class="online-status-text">Trực tuyến</span>
                </div>
              </div>
              <div class="user-avatar-dark">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path fill-rule="evenodd" d="M7.5 6a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0zM3.751 20.105a8.25 8.25 0 0116.498 0 .75.75 0 01-.437.695A18.683 18.683 0 0112 22.5c-2.786 0-5.433-.608-7.812-1.7a.75.75 0 01-.437-.695z" clip-rule="evenodd" />
                </svg>
                <span class="avatar-online-badge" title="Đang trực tuyến"></span>
              </div>
            </div>

            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <div class="dropdown-user-header">
                  <div class="user-avatar-dark">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                      <path fill-rule="evenodd" d="M7.5 6a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0zM3.751 20.105a8.25 8.25 0 0116.498 0 .75.75 0 01-.437.695A18.683 18.683 0 0112 22.5c-2.786 0-5.433-.608-7.812-1.7a.75.75 0 01-.437-.695z" clip-rule="evenodd" />
                    </svg>
                  </div>
                  <div class="dropdown-header-info">
                    <div class="dropdown-name">{{ userFullNameOrName }}</div>
                    <div class="dropdown-sub">@{{ authStore.user?.username }} ({{ authStore.user?.role }})</div>
                  </div>
                </div>

                <el-dropdown-item divided command="profile">
                  <el-icon><User /></el-icon>
                  <span>Thông tin tài khoản</span>
                </el-dropdown-item>

                <el-dropdown-item command="logout" class="dropdown-item-danger">
                  <el-icon><SwitchButton /></el-icon>
                  <span>Đăng xuất</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- MAIN CONTENT VIEW AREA -->
      <el-main class="content-body">
        <div class="content-container">
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>

    <!-- Dialog Cập nhật Thông tin Tài khoản -->
    <BaseDialog
      v-model="profileDialogVisible"
      title="Thông tin tài khoản"
      subtitle="Cập nhật ảnh đại diện, họ tên, số điện thoại và thông tin liên hệ cá nhân"
      width="540px"
      :loading="profileSaving"
      confirm-text="Lưu thay đổi"
      @confirm="saveProfile"
    >
      <el-form :model="profileForm" label-position="top" class="profile-form">
        <!-- Avatar Upload Section -->
        <div class="profile-avatar-row">
          <div class="avatar-preview-box">
            <el-avatar 
              :size="84" 
              :src="profileForm.avatarUrl" 
              class="avatar-preview-img"
            >
              {{ userInitial }}
            </el-avatar>
            <div 
              v-if="profileForm.avatarUrl" 
              class="avatar-delete-badge" 
              title="Xóa ảnh đại diện" 
              @click="profileForm.avatarUrl = ''"
            >
              <el-icon><Delete /></el-icon>
            </div>
          </div>

          <div class="avatar-upload-action">
            <div class="avatar-action-title">Ảnh đại diện</div>
            <div class="avatar-tip">Định dạng JPG, PNG, WEBP tối đa 5MB. Lưu trữ trực tiếp trên Cloudinary.</div>
            <el-upload
              action="#"
              :http-request="handleAvatarUpload"
              :show-file-list="false"
              accept=".jpg,.jpeg,.png,.webp,.gif"
              class="avatar-uploader"
            >
              <el-button type="primary" plain size="small" :loading="avatarUploading">
                <el-icon><UploadFilled /></el-icon>
                <span>{{ profileForm.avatarUrl ? 'Thay đổi ảnh đại diện' : 'Tải ảnh lên' }}</span>
              </el-button>
            </el-upload>
          </div>
        </div>

        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Tên đăng nhập">
              <el-input :value="'@' + (authStore.user?.username || '')" disabled>
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Vai trò hệ thống">
              <el-input :value="authStore.isAdmin ? 'Quản trị viên (ADMIN)' : 'Nhân viên (USER)'" disabled>
                <template #prefix><el-icon><Key /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Họ và tên">
              <el-input v-model="profileForm.fullName" placeholder="Ví dụ: Đỗ Lê" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Số điện thoại">
              <el-input v-model="profileForm.phone" placeholder="0912 345 678" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Địa chỉ Email">
          <el-input v-model="profileForm.email" placeholder="example@email.com">
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="Giới thiệu bản thân / Địa chỉ">
          <el-input 
            v-model="profileForm.bio" 
            type="textarea" 
            :rows="3" 
            maxlength="500" 
            show-word-limit
            placeholder="Nhập thông tin giới thiệu ngắn, địa chỉ nơi ở hoặc mô tả công việc..." 
          />
        </el-form-item>
      </el-form>
    </BaseDialog>

    <!-- Trợ lý ảo ERP AI Chatbot Widget (Nổi ở góc phải dưới) -->
    <ChatWidget ref="chatWidgetRef" />
  </el-container>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import BaseDialog from '../components/base/BaseDialog.vue'
import ChatWidget from '../components/chat/ChatWidget.vue'
import { useResponsive } from '../composables/useResponsive'
import {
  Monitor,
  Goods,
  User,
  BellFilled,
  Bell,
  Sunny,
  Moon,
  Delete,
  UploadFilled,
  Message,
  Key,
  SwitchButton,
  ArrowDown,
  Expand,
  Tickets,
  DataAnalysis,
  Setting,
  Cpu
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { isMobile } = useResponsive()

// State
const isCollapse = ref(false)
const mobileDrawerVisible = ref(false)
const isDaytime = ref(true)
const chatWidgetRef = ref(null)

// Clock State
const clockHours = ref('00')
const clockMinutes = ref('00')
const clockSeconds = ref('00')
const clockPeriod = ref('AM')
const clockDayName = ref('')
const clockDateFormatted = ref('')
let clockInterval = null

const isTopOfTheHour = computed(() => {
  return clockMinutes.value === '00' && parseInt(clockSeconds.value, 10) < 3
})

// Announcement
const announcementMessage = ref('Chào mừng đến với hệ thống ERP Cloud Enterprise • Đang đồng bộ dữ liệu thời gian thực')

// Profile Dialog
const profileDialogVisible = ref(false)
const profileSaving = ref(false)
const avatarUploading = ref(false)
const profileForm = reactive({
  fullName: '',
  phone: '',
  email: '',
  bio: '',
  avatarUrl: ''
})

const currentRouteTitle = computed(() => {
  if (route.path === '/') return 'Bảng điều khiển'
  if (route.path.startsWith('/products')) return 'Quản lý Sản phẩm'
  if (route.path.startsWith('/users')) return 'Quản lý Tài khoản'
  if (route.path.startsWith('/announcements')) return 'Quản lý Thông báo'
  return 'Hệ thống ERP'
})

const currentRouteBreadcrumb = computed(() => {
  if (route.path === '/') return 'Tổng quan Dashboard'
  if (route.path.startsWith('/products')) return 'Kho hàng & Sản phẩm'
  if (route.path.startsWith('/users')) return 'Danh sách nhân sự'
  if (route.path.startsWith('/announcements')) return 'Cài đặt truyền thông'
  return 'Trang chính'
})

const userFullNameOrName = computed(() => {
  return authStore.user?.fullName || authStore.user?.username || 'Admin'
})

const userInitial = computed(() => {
  const name = userFullNameOrName.value
  return name.charAt(0).toUpperCase()
})

function toggleSidebar() {
  isCollapse.value = !isCollapse.value
}

function toggleDayNight() {
  isDaytime.value = !isDaytime.value
}

function openChatWidget() {
  if (chatWidgetRef.value && chatWidgetRef.value.open) {
    chatWidgetRef.value.open()
  } else {
    // If widget has isOpen
    const widget = document.querySelector('.chat-floating-btn')
    if (widget) widget.click()
  }
}

function notifyFeature(name) {
  ElMessage.info({
    message: `Phân hệ [${name}] đang ở phiên bản Preview Enterprise.`,
    duration: 2500
  })
}

function focusGlobalSearch() {
  ElMessage({
    message: '⚡ Phím tắt tìm kiếm toàn hệ thống (⌘K) đã được kích hoạt!',
    type: 'success',
    duration: 2000
  })
}

function updateClock() {
  const now = new Date()
  const h = now.getHours()
  const m = now.getMinutes()
  const s = now.getSeconds()

  clockHours.value = String(h).padStart(2, '0')
  clockMinutes.value = String(m).padStart(2, '0')
  clockSeconds.value = String(s).padStart(2, '0')
  clockPeriod.value = h >= 12 ? 'PM' : 'AM'
  isDaytime.value = (h >= 6 && h < 18)

  const dayFormatter = new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    weekday: 'long'
  })
  const dateFormatter = new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })

  clockDayName.value = dayFormatter.format(now)
  clockDateFormatted.value = dateFormatter.format(now)
}

async function fetchAnnouncement() {
  try {
    const res = await api.get('/announcements/active')
    if (res.data && res.data.content) {
      announcementMessage.value = `${res.data.title || 'Thông báo'}: ${res.data.content}`
    }
  } catch {
    // Keep default
  }
}

function handleUserMenuCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?', 'Xác nhận đăng xuất', {
      confirmButtonText: 'Đăng xuất',
      cancelButtonText: 'Hủy',
      type: 'warning'
    }).then(() => {
      authStore.logout()
      ElMessage.success('Đã đăng xuất an toàn.')
      router.push('/login')
    }).catch(() => {})
  } else if (command === 'profile') {
    profileForm.fullName = authStore.user?.fullName || ''
    profileForm.phone = authStore.user?.phone || ''
    profileForm.email = authStore.user?.email || ''
    profileForm.bio = authStore.user?.bio || ''
    profileForm.avatarUrl = authStore.user?.avatarUrl || ''
    profileDialogVisible.value = true
  }
}

async function handleAvatarUpload(options) {
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    const res = await api.post('/users/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    profileForm.avatarUrl = res.data.avatarUrl
    if (authStore.user) {
      authStore.user.avatarUrl = res.data.avatarUrl
    }
    ElMessage.success('Tải ảnh đại diện lên thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Tải ảnh thất bại.')
  } finally {
    avatarUploading.value = false
  }
}

async function saveProfile() {
  profileSaving.value = true
  try {
    const res = await api.put('/users/profile', profileForm)
    authStore.user = { ...authStore.user, ...res.data }
    ElMessage.success('Cập nhật thông tin tài khoản thành công!')
    profileDialogVisible.value = false
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Cập nhật thất bại.')
  } finally {
    profileSaving.value = false
  }
}

onMounted(() => {
  updateClock()
  clockInterval = setInterval(updateClock, 1000)
  fetchAnnouncement()
})

onBeforeUnmount(() => {
  if (clockInterval) clearInterval(clockInterval)
})
</script>

<style scoped>
/* ==============================================================================
   FIGMA ENTERPRISE ADMIN LAYOUT STYLES (MATCHING IMAGES 3 & 4)
   ============================================================================== */
.admin-layout {
  min-height: 100vh;
  background-color: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #0f172a;
}

/* ==================== DESKTOP SIDEBAR ==================== */
.desktop-sidebar {
  background-color: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
  z-index: 10;
}

.sidebar-brand-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f1f5f9;
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  text-decoration: none;
}

.brand-icon {
  width: 36px;
  height: 36px;
  background: #2563eb;
  color: #ffffff;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
  flex-shrink: 0;
}

.brand-name-row {
  display: flex;
  align-items: center;
}

.brand-name {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.enterprise-tag {
  font-size: 9px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.6px;
  display: block;
  white-space: nowrap;
}

.btn-collapse-sidebar {
  background: none;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #64748b;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  padding: 0;
}

.btn-collapse-sidebar:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.nav-section-title {
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.8px;
  padding: 20px 20px 8px 20px;
}

/* Custom Enterprise Menu */
.custom-enterprise-menu {
  border-right: none;
  padding: 6px 10px;
  flex: 1;
}

:deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  border-radius: 10px;
  margin-bottom: 4px;
  color: #475569;
  font-weight: 500;
  font-size: 13.5px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  padding: 0 12px !important;
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
  color: #64748b;
  margin-right: 12px;
  transition: color 0.2s;
}

:deep(.el-menu-item:hover) {
  background-color: #f1f5f9;
  color: #0f172a;
}

:deep(.el-menu-item.is-active) {
  background-color: #2563eb !important;
  color: #ffffff !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

:deep(.el-menu-item.is-active .el-icon) {
  color: #ffffff !important;
}

.ai-menu-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.ai-pill-tag {
  font-size: 9px;
  font-weight: 800;
  background: #eff6ff;
  color: #2563eb;
  padding: 2px 6px;
  border-radius: 6px;
  border: 1px solid #bfdbfe;
}

:deep(.el-menu-item.is-active .ai-pill-tag) {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.4);
}

/* Sidebar Bottom Card */
.sidebar-bottom-card {
  margin: 12px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.server-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pulse-green-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
  flex-shrink: 0;
}

.server-title {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
}

.server-version {
  font-size: 10px;
  color: #64748b;
  margin-top: 2px;
  padding-left: 16px;
}

.sidebar-bottom-icon {
  padding: 16px;
  display: flex;
  justify-content: center;
}

/* ==================== MAIN WRAPPER & HEADER ==================== */
.main-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.top-header {
  height: 64px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  position: sticky;
  top: 0;
  z-index: 9;
}

.header-left-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-mobile-toggle {
  background: none;
  border: none;
  font-size: 20px;
  color: #475569;
  cursor: pointer;
  padding: 4px;
}

/* Inline Breadcrumb (ERP Core › Không gian làm việc) */
.header-breadcrumb-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
}

.bc-root {
  font-weight: 500;
  color: #64748b;
}

.bc-arrow {
  color: #94a3b8;
  font-size: 15px;
  font-weight: 600;
}

.bc-leaf {
  font-weight: 700;
  color: #1e293b;
}

/* Center Live Clock */
.header-center-clock {
  display: flex;
  align-items: center;
  justify-content: center;
}

.clean-clock-card {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  padding: 5px 14px 5px 8px;
  border-radius: 9999px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  user-select: none;
}

.clean-clock-card:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.clean-clock-card.is-top-hour {
  border-color: #818cf8;
  background: #f5f3ff;
  transform: scale(1.04);
}

.day-night-icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.is-day .day-night-icon {
  background: #fef3c7;
  color: #f59e0b;
}

.is-night .day-night-icon {
  background: #ede9fe;
  color: #6366f1;
}

.clock-content-wrap {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.clock-time-line {
  display: flex;
  align-items: baseline;
  gap: 1px;
}

.clock-digits {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.2px;
  transition: all 0.3s ease;
}

/* Phóng to kích thước chữ khi tròn giờ (trong 3 giây đầu: 12:00:00 - 12:00:02) */
.clock-digits.scale-digits {
  font-size: 17px;
  font-weight: 800;
  color: #4f46e5;
  transform: scale(1.08);
}

.clock-seconds {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
}

.clock-ampm {
  font-size: 9.5px;
  font-weight: 800;
  color: #64748b;
  margin-left: 4px;
  letter-spacing: 0.5px;
}

.clock-date-line {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #64748b;
}

.clock-day {
  font-weight: 600;
}

.clock-calendar {
  color: #475569;
}

.clock-zone-pill {
  font-size: 9px;
  font-weight: 700;
  background: #f1f5f9;
  color: #475569;
  padding: 1px 4px;
  border-radius: 4px;
  margin-left: 2px;
}

/* Header Right Group */
.header-right-group {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* Header Action Icons */
.header-icon-wrap {
  position: relative;
  cursor: pointer;
}

.btn-header-icon {
  background: none;
  border: none;
  color: #475569;
  cursor: pointer;
  padding: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-header-icon:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.bell-red-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 7px;
  height: 7px;
  background: #ef4444;
  border: 1.5px solid #ffffff;
  border-radius: 50%;
}

/* User Profile Trigger */
.user-profile-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 3px 6px;
  border-radius: 10px;
  transition: all 0.2s;
}

.user-profile-trigger:hover {
  background: #f8fafc;
}

.user-meta-block {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user-name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.user-status-online-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 1px 7px;
  border-radius: 12px;
  margin-top: 2px;
}

.online-pulse-dot {
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
}

.online-status-text {
  font-size: 10.5px;
  font-weight: 700;
  color: #15803d;
  letter-spacing: 0.2px;
}

.user-avatar-dark {
  width: 36px;
  height: 36px;
  background: #0f172a;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.15);
  flex-shrink: 0;
  position: relative;
}

.avatar-online-badge {
  position: absolute;
  bottom: 0px;
  right: 0px;
  width: 10px;
  height: 10px;
  background: #10b981;
  border: 2px solid #ffffff;
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(16, 185, 129, 0.3);
}

/* Dropdown Menu styling */
.dropdown-user-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.dropdown-header-info {
  text-align: left;
}

.dropdown-name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.dropdown-sub {
  font-size: 11px;
  color: #64748b;
}

.dropdown-item-danger {
  color: #ef4444 !important;
}

/* ==================== ANNOUNCEMENT MARQUEE BAR ==================== */
.system-announcement-bar {
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  height: 38px;
  display: flex;
  align-items: center;
  overflow: hidden;
  padding: 0 16px;
}

.announcement-inner {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 12px;
}

.announcement-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 3px 8px;
  border-radius: 6px;
  color: #1d4ed8;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.pulse-indicator {
  width: 6px;
  height: 6px;
  background: #2563eb;
  border-radius: 50%;
}

.announcement-marquee-wrapper {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
}

.announcement-track {
  display: inline-block;
  white-space: nowrap;
  animation: marqueeAnim 35s linear infinite;
}

.announcement-item {
  font-size: 12.5px;
  color: #334155;
  font-weight: 500;
}

.announcement-separator {
  margin: 0 16px;
  color: #3b82f6;
  font-size: 10px;
}

@keyframes marqueeAnim {
  0% { transform: translateX(0%); }
  100% { transform: translateX(-50%); }
}

/* ==================== CONTENT BODY ==================== */
.content-body {
  padding: 24px;
  flex: 1;
  background: #f8fafc;
}

.content-container {
  max-width: 1440px;
  margin: 0 auto;
}

/* Profile Form modal */
.profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.avatar-preview-box {
  position: relative;
}

.avatar-preview-img {
  background: #2563eb;
  color: #ffffff;
  font-size: 32px;
  font-weight: 700;
}

.avatar-delete-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: #ffffff;
  border-radius: 50%;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.avatar-action-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.avatar-tip {
  font-size: 12px;
  color: #64748b;
  margin: 4px 0 10px 0;
}

@media (max-width: 768px) {
  .top-header {
    padding: 0 14px;
  }
  .content-body {
    padding: 16px 12px;
  }
}
</style>
