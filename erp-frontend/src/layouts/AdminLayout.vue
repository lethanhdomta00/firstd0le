<template>
  <el-container class="admin-layout">
    <!-- DESKTOP SIDEBAR (Visible on >= 992px) -->
    <el-aside 
      v-if="!isMobile" 
      :width="isCollapse ? '64px' : '230px'" 
      class="desktop-sidebar"
    >
      <div class="sidebar-logo">
        <el-icon class="logo-icon"><Goods /></el-icon>
        <span v-if="!isCollapse" class="logo-text">ERP System</span>
      </div>

      <el-menu 
        :default-active="$route.path" 
        router 
        :collapse="isCollapse"
        background-color="#1e293b" 
        text-color="#94a3b8" 
        active-text-color="#38bdf8"
        class="custom-menu"
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
    </el-aside>

    <!-- MOBILE DRAWER SIDEBAR (Visible on < 992px) -->
    <el-drawer
      v-else
      v-model="mobileDrawerVisible"
      direction="ltr"
      size="260px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-header">
        <div class="sidebar-logo">
          <el-icon class="logo-icon"><Goods /></el-icon>
          <span class="logo-text">ERP System</span>
        </div>
      </div>

      <el-menu 
        :default-active="$route.path" 
        router 
        background-color="#1e293b" 
        text-color="#94a3b8" 
        active-text-color="#38bdf8"
        class="custom-menu"
        @select="mobileDrawerVisible = false"
      >
        <el-menu-item index="/">
          <el-icon><Monitor /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>Quản lý Sản phẩm</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>Quản lý Tài khoản</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/announcements">
          <el-icon><BellFilled /></el-icon>
          <span>Quản lý Thông báo</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <!-- MAIN APP CONTENT AREA -->
    <el-container class="main-wrapper">
      <!-- HEADER -->
      <el-header class="top-header">
        <div class="header-left">
          <!-- Toggle button for desktop collapse or mobile drawer -->
          <el-button 
            type="text" 
            class="toggle-btn"
            @click="toggleSidebar"
          >
            <el-icon :size="20">
              <Expand v-if="isMobile ? !mobileDrawerVisible : isCollapse" />
              <Fold v-else />
            </el-icon>
          </el-button>

          <!-- Current Route Title -->
          <span class="page-current-title">{{ currentRouteTitle }}</span>
        </div>

        <!-- VỊ TRÍ 1: ĐỒNG HỒ THỜI GIAN THỰC (MÚI GIỜ +7) -->
        <div class="header-center">
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

        <!-- COLLAPSED HEADER RIGHT: SINGLE USER DROPDOWN -->
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleUserMenuCommand" placement="bottom-end">
            <div class="user-dropdown-trigger">
              <el-avatar 
                :size="34" 
                :src="authStore.user?.avatarUrl" 
                class="user-avatar"
              >
                {{ userInitial }}
              </el-avatar>
              <div class="user-details-wrap">
                <span class="username-text">{{ userFullNameOrName }}</span>
                <span class="user-role-badge" :class="authStore.isAdmin ? 'badge-admin' : 'badge-user'">
                  {{ authStore.isAdmin ? 'ADMIN' : 'STAFF' }}
                </span>
              </div>
              <el-icon class="dropdown-caret"><ArrowDown /></el-icon>
            </div>

            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <!-- Dropdown Header Info Card -->
                <div class="dropdown-user-header">
                  <el-avatar 
                    :size="42" 
                    :src="authStore.user?.avatarUrl" 
                    class="dropdown-header-avatar"
                  >
                    {{ userInitial }}
                  </el-avatar>
                  <div class="dropdown-header-info">
                    <div class="dropdown-name">{{ userFullNameOrName }}</div>
                    <div class="dropdown-sub">@{{ authStore.user?.username }}</div>
                    <div v-if="authStore.user?.email" class="dropdown-email">{{ authStore.user?.email }}</div>
                  </div>
                </div>

                <!-- Menu Item: Thông tin tài khoản -->
                <el-dropdown-item divided command="profile">
                  <el-icon><User /></el-icon>
                  <span>Thông tin tài khoản</span>
                </el-dropdown-item>

                <!-- Menu Item: Đăng xuất -->
                <el-dropdown-item command="logout" class="dropdown-item-danger">
                  <el-icon><SwitchButton /></el-icon>
                  <span>Đăng xuất</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- VỊ TRÍ 2: THANH THÔNG BÁO / SỰ KIỆN HỆ THỐNG (CHẠY CHỮ MARQUEE LIÊN TỤC) -->
      <div class="system-announcement-bar">
        <div class="announcement-inner">
          <div class="announcement-badge">
            <span class="pulse-indicator"></span>
            <el-icon class="announcement-icon"><BellFilled /></el-icon>
            <span class="badge-title">THÔNG BÁO</span>
          </div>
          <div class="announcement-marquee-wrapper">
            <div class="announcement-track">
              <span class="announcement-item">{{ announcementMessage }}</span>
              <span class="announcement-separator">✦</span>
              <span class="announcement-item">{{ announcementMessage }}</span>
              <span class="announcement-separator">✦</span>
            </div>
          </div>
        </div>
      </div>

      <!-- CONTENT BODY -->
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
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useResponsive } from '../composables/useResponsive'
import { ElMessage } from 'element-plus'
import api from '../api'
import { 
  Monitor, 
  Goods, 
  User, 
  Fold, 
  Expand, 
  SwitchButton, 
  ArrowDown, 
  Delete, 
  UploadFilled, 
  Key, 
  Message,
  Clock,
  BellFilled,
  Sunny,
  Moon
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const { isMobile } = useResponsive()

const isCollapse = ref(false)
const mobileDrawerVisible = ref(false)

// Vị trí 1: Đồng hồ thời gian thực (Múi giờ Việt Nam GMT+7)
const clockHours = ref('00')
const clockMinutes = ref('00')
const clockSeconds = ref('00')
const clockPeriod = ref('')
const clockDayName = ref('')
const clockDateFormatted = ref('')
let clockTimer = null

const updateClock = () => {
  const now = new Date()
  const timeParts = new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).formatToParts(now)

  const h = timeParts.find(p => p.type === 'hour')?.value || '00'
  const m = timeParts.find(p => p.type === 'minute')?.value || '00'
  const s = timeParts.find(p => p.type === 'second')?.value || '00'

  clockHours.value = h
  clockMinutes.value = m
  clockSeconds.value = s
  clockPeriod.value = parseInt(h, 10) >= 12 ? 'PM' : 'AM'

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

// Kiểm tra ban ngày (6:00 - 18:00) hay ban đêm (18:00 - 6:00)
const isDaytime = computed(() => {
  const h = parseInt(clockHours.value, 10)
  return h >= 6 && h < 18
})

// Kiểm tra tròn giờ (ví dụ: 12:00:00, 13:00:00 -> diễn ra trong đúng 3 giây đầu rồi tự tắt)
const isTopOfTheHour = computed(() => {
  return clockMinutes.value === '00' && parseInt(clockSeconds.value, 10) < 3
})

// Vị trí 2: Dòng chữ thông báo hệ thống (Lấy từ API và cập nhật tự động)
const announcementMessage = ref(
  '🎉 Chào mừng bạn đến với ERP Cloud System! Chúc bạn một ngày làm việc hiệu quả, năng lượng và hoàn thành tốt mọi mục tiêu.'
)

const fetchActiveAnnouncement = async () => {
  try {
    const res = await api.get('/announcements/active')
    if (res.data && res.data.content) {
      announcementMessage.value = res.data.content
    }
  } catch (e) {
    // Keep fallback default
  }
}

// Profile Modal State
const profileDialogVisible = ref(false)
const profileSaving = ref(false)
const avatarUploading = ref(false)

const profileForm = reactive({
  fullName: '',
  email: '',
  phone: '',
  avatarUrl: '',
  bio: ''
})

onMounted(() => {
  if (authStore.isAuthenticated && !authStore.user) {
    authStore.fetchCurrentUser()
  }
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  fetchActiveAnnouncement()
  window.addEventListener('announcement-updated', fetchActiveAnnouncement)
})

onUnmounted(() => {
  if (clockTimer) {
    clearInterval(clockTimer)
  }
  window.removeEventListener('announcement-updated', fetchActiveAnnouncement)
})

const displayName = computed(() => authStore.user?.username || 'Admin')
const userFullNameOrName = computed(() => authStore.user?.fullName || authStore.user?.username || 'Admin')
const userInitial = computed(() => {
  const name = userFullNameOrName.value
  return name ? name.charAt(0).toUpperCase() : 'A'
})

const toggleSidebar = () => {
  if (isMobile.value) {
    mobileDrawerVisible.value = !mobileDrawerVisible.value
  } else {
    isCollapse.value = !isCollapse.value
  }
}

const currentRouteTitle = computed(() => {
  if (route.path === '/products') return 'Quản lý Sản phẩm'
  if (route.path === '/users') return 'Quản lý Tài khoản'
  if (route.path === '/announcements') return 'Quản lý Thông báo & Sự kiện'
  return 'Tổng quan Dashboard'
})

const handleUserMenuCommand = (command) => {
  if (command === 'profile') {
    openProfileDialog()
  } else if (command === 'logout') {
    handleLogout()
  }
}

const openProfileDialog = () => {
  profileForm.fullName = authStore.user?.fullName || ''
  profileForm.email = authStore.user?.email || ''
  profileForm.phone = authStore.user?.phone || ''
  profileForm.avatarUrl = authStore.user?.avatarUrl || ''
  profileForm.bio = authStore.user?.bio || ''
  profileDialogVisible.value = true
}

const handleAvatarUpload = async ({ file, onSuccess, onError }) => {
  avatarUploading.value = true
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    profileForm.avatarUrl = res.data.url
    ElMessage.success('Tải ảnh đại diện lên Cloudinary thành công!')
    onSuccess(res.data)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi tải ảnh đại diện lên Cloud!')
    onError(err)
  } finally {
    avatarUploading.value = false
  }
}

const saveProfile = async () => {
  profileSaving.value = true
  try {
    const res = await authStore.updateProfile(profileForm)
    if (res.success) {
      ElMessage.success('Cập nhật thông tin tài khoản thành công!')
      profileDialogVisible.value = false
    } else {
      ElMessage.error(res.message || 'Lỗi khi cập nhật thông tin!')
    }
  } catch (err) {
    ElMessage.error('Đã xảy ra lỗi khi lưu thông tin!')
  } finally {
    profileSaving.value = false
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  width: 100%;
  background-color: #f8fafc;
}

/* Desktop Sidebar */
.desktop-sidebar {
  background-color: #1e293b;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow-x: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background-color: #0f172a;
  color: #38bdf8;
  font-weight: 700;
  font-size: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
}

.logo-icon {
  font-size: 22px;
}

.custom-menu {
  border-right: none;
  flex: 1;
}

.custom-menu :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  margin: 4px 8px;
  border-radius: 8px;
}

.custom-menu :deep(.el-menu-item.is-active) {
  background-color: #0284c7 !important;
  color: #ffffff !important;
  font-weight: 600;
}

/* Mobile Drawer */
:deep(.mobile-drawer .el-drawer__body) {
  padding: 0;
  background-color: #1e293b;
}

.drawer-header {
  background-color: #0f172a;
}

/* Main Area */
.main-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;
  overflow-x: hidden;
}

/* Top Header */
.top-header {
  height: 60px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 99;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-btn {
  color: #475569 !important;
  padding: 8px !important;
  font-size: 18px;
  border-radius: 8px;
}

.toggle-btn:hover {
  background-color: #f1f5f9;
}

.page-current-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

/* Header Center - Vị trí 1: Đồng hồ Tinh Gọn (Không Đổ Bóng, Icon Ngày/Đêm & Phóng To Tròn Giờ) */
.header-center {
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
  box-shadow: none !important;
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  user-select: none;
}

.clean-clock-card:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

/* Hiệu ứng khi Tròn giờ (Ví dụ: 12:00, 13:00) */
.clean-clock-card.is-top-hour {
  border-color: #818cf8;
  background: #f5f3ff;
  transform: scale(1.06);
}

/* Icon Mặt trời / Mặt trăng theo ban ngày (6:00-18:00) và ban đêm (18:00-6:00) */
.day-night-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
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
  line-height: 1.2;
}

.clock-time-line {
  display: flex;
  align-items: center;
  gap: 3px;
}

.clock-digits {
  font-family: 'SF Mono', Monaco, 'Segoe UI Mono', monospace;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: inline-block;
}

/* Phóng to thời gian khi tròn giờ (trong 3 giây) */
.clock-digits.scale-digits {
  font-size: 17px;
  font-weight: 800;
  color: #4f46e5;
  transform: scale(1.08);
}

.clock-seconds {
  font-family: 'SF Mono', Monaco, 'Segoe UI Mono', monospace;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

.clock-ampm {
  font-size: 9px;
  font-weight: 700;
  color: #64748b;
  margin-left: 2px;
  background: #f1f5f9;
  padding: 0 4px;
  border-radius: 3px;
}

.clock-date-line {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
}

.clock-day {
  color: #64748b;
  font-weight: 600;
}

.clock-calendar {
  color: #475569;
  font-weight: 500;
}

.clock-zone-pill {
  background: #f1f5f9;
  color: #0284c7;
  font-size: 9px;
  font-weight: 700;
  padding: 0 4px;
  border-radius: 3px;
  letter-spacing: 0.5px;
}

/* Vị trí 2: Thanh Thông Báo / Sự Kiện Hệ Thống (Marquee Chạy Chữ Mượt Mà) */
.system-announcement-bar {
  width: 100%;
  background: linear-gradient(90deg, #ecfdf5 0%, #eff6ff 50%, #fffbeb 100%);
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 16px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 10;
  overflow: hidden;
}

.announcement-inner {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.announcement-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  padding: 3px 10px;
  border-radius: 9999px;
  border: 1px solid #cbd5e1;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  z-index: 2;
}

.pulse-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ef4444;
  box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
  animation: pulse-ring 1.8s infinite;
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(239, 68, 68, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0);
  }
}

.announcement-icon {
  font-size: 14px;
  color: #f59e0b;
}

.badge-title {
  font-size: 11px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.5px;
}

/* MARQUEE RUNNING TEXT */
.announcement-marquee-wrapper {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  position: relative;
  mask-image: linear-gradient(to right, transparent, black 15px, black 95%, transparent);
  -webkit-mask-image: linear-gradient(to right, transparent, black 15px, black 95%, transparent);
}

.announcement-track {
  display: inline-flex;
  align-items: center;
  gap: 32px;
  white-space: nowrap;
  animation: marquee-scroll 28s linear infinite;
  will-change: transform;
}

.announcement-track:hover {
  animation-play-state: paused;
}

.announcement-item {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  cursor: default;
}

.announcement-separator {
  color: #f59e0b;
  font-size: 10px;
}

@keyframes marquee-scroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

/* Header Right - User Dropdown Trigger */
.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 12px 5px 6px;
  background-color: #f8fafc;
  border-radius: 9999px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.user-dropdown-trigger:hover {
  background-color: #f1f5f9;
  border-color: #cbd5e1;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.user-details-wrap {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-avatar {
  background: linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.username-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  max-width: 130px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role-badge {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
  border-radius: 4px;
  padding: 1px 4px;
  display: inline-block;
  margin-top: 1px;
  width: fit-content;
}

.badge-admin {
  background: #fee2e2;
  color: #ef4444;
}

.badge-user {
  background: #e0f2fe;
  color: #0284c7;
}

.dropdown-caret {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 2px;
  transition: transform 0.2s ease;
}

/* Dropdown Menu Header */
.dropdown-user-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  min-width: 220px;
}

.dropdown-header-avatar {
  background: linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}

.dropdown-header-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dropdown-name {
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
}

.dropdown-sub {
  font-size: 12px;
  color: #64748b;
}

.dropdown-email {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}

.dropdown-item-danger {
  color: #ef4444 !important;
}

.dropdown-item-danger:hover {
  background-color: #fef2f2 !important;
  color: #dc2626 !important;
}

/* Profile Form Avatar Upload Styling */
.profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px;
  background-color: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  margin-bottom: 18px;
}

.avatar-preview-box {
  position: relative;
  flex-shrink: 0;
}

.avatar-preview-img {
  background: linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%);
  color: white;
  font-weight: 700;
  font-size: 28px;
  border: 2px solid #ffffff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.avatar-delete-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 24px;
  height: 24px;
  background: #ef4444;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  border: 2px solid white;
  transition: transform 0.2s ease;
}

.avatar-delete-badge:hover {
  transform: scale(1.1);
  background: #dc2626;
}

.avatar-upload-action {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.avatar-action-title {
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
}

.avatar-tip {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

/* Main Content */
.content-body {
  padding: 24px;
  flex: 1;
  width: 100%;
  box-sizing: border-box;
}

.content-container {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

/* Responsive Breakpoints */
@media (max-width: 992px) {
  .clock-date, .clock-divider, .clock-tz-badge {
    display: none;
  }
}

@media (max-width: 768px) {
  .header-center {
    display: none;
  }

  .top-header {
    padding: 0 12px;
  }

  .content-body {
    padding: 12px;
  }

  .page-current-title {
    font-size: 14px;
  }

  .username-text {
    display: none;
  }

  .user-role-badge {
    display: none;
  }

  .dropdown-caret {
    display: none;
  }

  .user-dropdown-trigger {
    padding: 2px;
    border-radius: 50%;
  }

  .profile-avatar-row {
    flex-direction: column;
    text-align: center;
  }
}
</style>
